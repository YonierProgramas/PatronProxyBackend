#!/usr/bin/env python3
"""Real HTTP acceptance checks; requires four running applications and an empty audit DB."""
import json
import os
from urllib.request import Request, urlopen
from urllib.error import HTTPError
BASE = os.environ.get('API_BASE_URL','http://localhost:8080')
def call(path, body=None):
    req = Request(BASE + path, data=None if body is None else json.dumps(body).encode(), headers={'Content-Type':'application/json'})
    try:
        with urlopen(req, timeout=180) as response:
            return response.status, json.load(response)
    except HTTPError as error:
        return error.code, json.load(error)
def total():
    status, summary = call('/api/metrics/summary')
    assert status == 200
    return sum(item['totalCalls'] for item in summary)
assert total() == 0, 'Use a fresh H2 DB: do not delete an existing demonstration DB.'
status, result = call('/api/services/inventory/getStock', {'params':[{'productId':'SKU-001'}]})
assert status == 200 and result['response']['stock'] == 30, f'Expected initial stock 30; HTTP {status}: {result}'
manual_id = result['requestId']
status, page = call('/api/metrics/logs?service=inventory&status=SUCCESS&page=0&size=10')
assert status == 200 and page['content'][0]['requestId'] == manual_id
assert total() == 1
status, batch = call('/api/metrics/simulate-load', {})
assert status == 200 and batch['requestedCalls'] == batch['completedCalls'] == 50
assert batch['successCount'] + batch['errorCount'] == 50
assert total() == 51
status, page = call('/api/metrics/logs?page=0&size=100')
assert status == 200 and page['totalElements'] == 51
assert len({log['requestId'] for log in page['content']}) == 51
status, summary = call('/api/metrics/summary')
assert {s['serviceId']:s['totalCalls'] for s in summary} == {'inventory':18,'orders':17,'payments':16}
for service in summary:
    assert service['totalCalls'] == service['successCount']+service['errorCount']
    assert service['averageDurationMs'] >= 0
    assert service['recentCalls'] == sorted(service['recentCalls'],key=lambda log:log['timestamp'])
status, invalid = call('/api/services/inventory/removeStock', {'params':[{'productId':'SKU-001','quantity':100000}]})
assert status == 409 and invalid['requestId'] and invalid['errorCode'] == 'INSUFFICIENT_STOCK'
status, unknown = call('/api/services/orders/unknown', {'params':[]})
assert status == 400 and unknown['requestId']
status, invalid_filter = call('/api/metrics/logs?from=2026-01-02T00:00:00Z&to=2026-01-01T00:00:00Z')
assert status == 400
status, too_big = call('/api/services/inventory/getStock', {'params':[{'productId':'x'*40000}]})
assert status == 413 and too_big['requestId']
print(json.dumps({'result':'PASS','manualRequestId':manual_id,'batch':batch,'totalAfterBatch':51,'summary':[{k:v for k,v in item.items() if k!='recentCalls'} for item in summary]},indent=2))
