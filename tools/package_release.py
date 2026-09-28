#!/usr/bin/env python3
"""Package the complete repository project, assets, credits, evidence and install JAR."""
from pathlib import Path
import zipfile,hashlib,subprocess,sys
root=Path(__file__).resolve().parents[1]
subprocess.run([sys.executable,str(root/'tools/validate_release.py')],check=True)
files={}
for folder in ['src','assets','tools','third_party','preview','docs']:
 for p in (root/folder).rglob('*'):
  if p.is_file() and '__pycache__' not in p.parts:
   rel=p.relative_to(root).as_posix()
   files[rel]=p.read_bytes()
for rel in ['README.md','CREDITS.md','NOTICE.md','LICENSE','.gitignore','PROVENANCE.md','build.sh','icon.png','mod.hjson','release/Wither-Storm-v1.7.1.jar']:files[rel]=(root/rel).read_bytes()
files['MANIFEST.sha256']=''.join(hashlib.sha256(v).hexdigest()+'  '+k+'\n' for k,v in sorted(files.items())).encode()
out=root/'release/凋零风暴_v1.7.1_完整包.zip'
with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
 for k,v in sorted(files.items()):z.writestr('Wither-Storm-v1.7.1/'+k,v)
with zipfile.ZipFile(out) as z:assert z.testzip() is None
print(out)
