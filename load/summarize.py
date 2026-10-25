import csv,sys,json,math
from pathlib import Path
rows=[r for r in csv.DictReader(open(sys.argv[1])) if r['label']=='Reserva concorrente']
accepted=sum(r['responseCode']=='200' for r in rows);conflicts=sum(r['responseCode']=='409' for r in rows)
latencies=sorted(int(r['elapsed']) for r in rows)
result={'requests':len(rows),'accepted':accepted,'conflicts':conflicts,'other':len(rows)-accepted-conflicts,'p95_ms':latencies[max(0,math.ceil(len(rows)*.95)-1)] if rows else None,'consistency_pass':len(rows)>1 and accepted==1 and conflicts==len(rows)-1}
print(json.dumps(result,indent=2));Path('load/results').mkdir(exist_ok=True);Path('load/results/summary.json').write_text(json.dumps(result,indent=2))
sys.exit(0 if result['consistency_pass'] else 1)
