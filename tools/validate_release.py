from pathlib import Path
import json,zipfile,hashlib
P=Path(__file__).resolve().parents[1];r=json.loads((P/'preview/release-validation.json').read_text());jar=P/'release/Wither-Storm-v1.7.1.jar'
assert r['headlessPassed']==217 and hashlib.sha256(jar.read_bytes()).hexdigest()==r['jarSHA256']
assert all(x['passed'] for x in r['beamPixels'].values())
for x in r['layout'].values():
 assert x['actualSceneDragEvents'] and x['positionsReloadedFromSettingsFile'] and x['offscreenPositionClamped'] and x['normalModeDoesNotInterceptInput']
 assert all(y['overlaps']==0 and y['visiblePanels']==2 for y in x['viewports'])
for x in r['aim'].values():
 for k in ['threeFrontAperturesActive','rearBeamAndPullBlocked','translationDuringTurn','firesAfterAlignment','windupBeforeDamage','animatedImpact','twoTeamPanels','displayToggles']:assert x[k]
with zipfile.ZipFile(jar) as z:
 assert z.testzip() is None and 'classes.dex' in z.namelist() and 'wstorm/gfx/HudLayout.class' in z.namelist()
 assert 'version: "1.7.1"' in z.read('mod.hjson').decode()
 assert 'author: "sayed1145"' in z.read('mod.hjson').decode()
 assert 'NLM-2b' in z.read('credits/CREDITS.md').decode()
print('v1.7.1 install JAR / shipped evidence integrity PASS; not a gameplay rerun.')
