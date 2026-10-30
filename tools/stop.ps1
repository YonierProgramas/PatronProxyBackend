$ErrorActionPreference = 'Stop'
if (-not (Get-Command py -ErrorAction SilentlyContinue)) { throw 'Instala o habilita Python 3 (py) para estos scripts.' }
py -3 "$PSScriptRoot/manage.py" stop
if ($LASTEXITCODE -ne 0) { throw 'La operación falló; revisa los mensajes y logs.' }
