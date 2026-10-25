"""Prepare cenário isolado. Credenciais recebidas por ambiente, não pela linha de comando."""
import os,json,urllib.request,http.cookiejar,datetime,pathlib
base=os.getenv('BASE_URL','http://localhost:8080');user=os.getenv('LOAD_USER','admin');password=os.environ['ADMIN_PASSWORD']
opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
def call(path,data=None,headers=None):
 request=urllib.request.Request(base+path,data=data,headers=headers or {})
 with opener.open(request,timeout=15) as r:
  text=r.read();return json.loads(text) if text else None
csrf=call('/api/csrf')
import urllib.parse
call('/api/login',urllib.parse.urlencode({'username':user,'password':password}).encode(),{csrf['headerName']:csrf['token'],'Content-Type':'application/x-www-form-urlencoded'})
csrf=call('/api/csrf')
resource=call('/api/resources',json.dumps({'name':'Carga '+datetime.datetime.now().isoformat(),'type':'SALA','capacity':40,'location':'Laboratório de carga','competence':'','active':True,'restricted':False}).encode(),{csrf['headerName']:csrf['token'],'Content-Type':'application/json'})
start=datetime.datetime.now(datetime.timezone.utc)+datetime.timedelta(days=7);end=start+datetime.timedelta(hours=1)
url=urllib.parse.urlparse(base)
values={'host':url.hostname,'port':str(url.port or (443 if url.scheme=='https' else 80)),'protocol':url.scheme,'user':user,'password':password,'resourceId':str(resource['id']),'start':start.isoformat(),'end':end.isoformat()}
def escape(value):return value.replace('\\','\\\\').replace('\n','\\n').replace('\r','\\r').replace('=','\\=').replace(':','\\:')
path=pathlib.Path('load/local.properties');path.write_text('\n'.join(k+'='+escape(v) for k,v in values.items())+'\n');path.chmod(0o600)
print('Cenário preparado: recurso',resource['id'],'. Execute JMeter com -q load/local.properties. Não publique esse arquivo.')
