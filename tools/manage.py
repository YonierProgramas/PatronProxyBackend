#!/usr/bin/env python3
"""Launch only project-owned JARs and stop only processes with verified command lines."""
import argparse
import json
import os
from pathlib import Path
import shutil
import signal
import socket
import subprocess
import sys
import time
from urllib.request import urlopen
ROOT = Path(__file__).resolve().parents[1]
RUNTIME = ROOT / '.runtime'
MODULES = ['inventory-service', 'order-service', 'payment-service', 'monitor-api']
DEFAULT_PORTS = [8081, 8082, 8083, 8080]
PORT_KEYS = ['INVENTORY_PORT', 'ORDER_PORT', 'PAYMENT_PORT', 'MONITOR_PORT']
def command_line(pid):
    try:
        if os.name == 'nt':
            return subprocess.check_output(['powershell.exe', '-NoProfile', '-Command', f'(Get-CimInstance Win32_Process -Filter "ProcessId={int(pid)}").CommandLine'], text=True).strip()
        return Path(f'/proc/{pid}/cmdline').read_bytes().replace(b'\0', b' ').decode()
    except (OSError, subprocess.CalledProcessError):
        return ''
def stop():
    if not RUNTIME.exists():
        return
    for module in reversed(MODULES):
        state = RUNTIME / f'{module}.json'
        if not state.exists():
            continue
        data = json.loads(state.read_text())
        pid, jar = data['pid'], data['jar']
        cmd = command_line(pid)
        if cmd and jar in cmd and 'java' in cmd.lower():
            os.kill(pid, signal.SIGTERM)
            deadline = time.monotonic() + 20
            while command_line(pid) and time.monotonic() < deadline:
                time.sleep(.2)
            if command_line(pid):
                raise RuntimeError(f'{module} is still stopping. Wait before rebuilding.')
            print(f'Stopped {module}: {pid}')
            state.unlink()
        elif not cmd:
            state.unlink()
        else:
            print(f'Refusing to stop PID {pid}: ownership mismatch.', file=sys.stderr)
def start():
    if not shutil.which('java'):
        raise RuntimeError('Java 21 is required on PATH. Set JAVA_HOME and PATH first.')
    version = subprocess.check_output(['java', '-version'], stderr=subprocess.STDOUT, text=True)
    if 'version "21.' not in version:
        raise RuntimeError('Use Java 21. Found: ' + version.splitlines()[0])
    ports = [int(os.environ.get(key, default)) for key, default in zip(PORT_KEYS, DEFAULT_PORTS)]
    if len(set(ports)) != 4:
        raise RuntimeError('All four ports must be distinct.')
    for module, port in zip(MODULES, ports):
        jar = ROOT / module / 'target' / f'{module}-1.0.0.jar'
        if not jar.exists():
            raise RuntimeError(f'Build first: missing {jar}')
        state = RUNTIME / f'{module}.json'
        if state.exists() and command_line(json.loads(state.read_text())['pid']):
            raise RuntimeError(f'{module} already has a tracked process. Stop it first.')
        with socket.socket() as sock:
            sock.bind(('127.0.0.1', port))
    RUNTIME.mkdir(exist_ok=True)
    env = dict(os.environ)
    for key, port in zip(['INVENTORY_SERVICE_URL','ORDER_SERVICE_URL','PAYMENT_SERVICE_URL'], ports[:3]):
        env.setdefault(key, f'http://localhost:{port}')
    started = []
    try:
        for module, port in zip(MODULES, ports):
            jar = str((ROOT / module / 'target' / f'{module}-1.0.0.jar').resolve())
            log_path = RUNTIME / f'{module}.log'
            with log_path.open('ab') as log:
                child = subprocess.Popen(['java','-Xms32m','-Xmx192m','-XX:ActiveProcessorCount=2','-jar',jar], cwd=ROOT, env={**env,'PORT':str(port)}, stdout=log, stderr=subprocess.STDOUT, start_new_session=os.name != 'nt')
            started.append(child)
            (RUNTIME / f'{module}.json').write_text(json.dumps({'pid':child.pid,'jar':jar,'port':port}))
            deadline = time.monotonic() + 60
            while time.monotonic() < deadline:
                if child.poll() is not None:
                    raise RuntimeError(f'{module} exited. Inspect {log_path}')
                try:
                    with urlopen(f'http://localhost:{port}/actuator/health',timeout=1) as response:
                        if response.status == 200:
                            break
                except OSError:
                    time.sleep(.3)
            else:
                raise RuntimeError(f'{module} health timeout. Inspect {log_path}')
            print(f'{module}: http://localhost:{port} | log: {log_path}', flush=True)
    except BaseException:
        for child in started:
            if child.poll() is None:
                child.terminate()
        raise
if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('action',choices=['start','stop','status'])
    args = parser.parse_args()
    try:
        if args.action == 'start':
            start()
        elif args.action == 'stop':
            stop()
        else:
            for module in MODULES:
                state = RUNTIME / f'{module}.json'
                print(module, state.read_text() if state.exists() else 'not tracked')
    except Exception as error:
        print(str(error),file=sys.stderr)
        sys.exit(1)
