import os, re, sys
SRC = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(os.path.dirname(SRC), 'project')
os.makedirs(OUT, exist_ok=True)
css = open(os.path.join(SRC, 'shared.css')).read()
data = open(os.path.join(SRC, 'shared.js')).read()
ICON = {
 'overview': '<path d="M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z"></path>',
 'map': '<circle cx="18" cy="5" r="3"></circle><circle cx="6" cy="12" r="3"></circle><circle cx="18" cy="19" r="3"></circle><path d="M8.6 13.5l6.8 4M15.4 6.5l-6.8 4"></path>',
 'drift': '<circle cx="18" cy="18" r="3"></circle><circle cx="6" cy="6" r="3"></circle><path d="M13 6h3a2 2 0 0 1 2 2v7M11 18H8a2 2 0 0 1-2-2V9"></path>',
 'findings': '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path><path d="M12 8v4M12 16h.01"></path>',
 'agents': '<rect x="4" y="4" width="16" height="16" rx="2"></rect><rect x="9" y="9" width="6" height="6"></rect><path d="M9 1v3M15 1v3M9 20v3M15 20v3M20 9h3M20 14h3M1 9h3M1 14h3"></path>',
 'theme': '<path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"></path>',
}
NAV = [('overview','Overview','Overview.dc.html'),('map','Map','Service-map.dc.html'),('drift','Drift','Drift.dc.html'),('findings','Findings','Findings.dc.html'),('agents','Agents','Agents.dc.html')]
def rail(current):
    items = []
    for key,label,href in NAV:
        cur = ' aria-current="page"' if key == current else ''
        items.append(f'  <a class="rail-item" href="{href}"{cur}><svg class="ico" viewBox="0 0 24 24" aria-hidden="true">{ICON[key]}</svg><span>{label}</span></a>')
    logo = '<svg class="ico" viewBox="0 0 24 24" aria-hidden="true" style="width: 22px; height: 22px"><circle cx="5" cy="12" r="2.2"></circle><circle cx="12" cy="5" r="2.2"></circle><circle cx="19" cy="12" r="2.2"></circle><circle cx="12" cy="19" r="2.2"></circle><path d="M7 11l3.5-4.5M14 6.5l3.5 4.5M17 13.5L13.5 18M10.5 18L7 13.5"></path></svg>'
    return ('<nav class="rail" aria-label="Primary">\n  <a class="rail-logo" href="Overview.dc.html" aria-label="Architrace">' + logo + '</a>\n' + '\n'.join(items) +
            '\n  <span style="flex: 1"></span>\n  <button class="rail-item" onClick="{{toggleTheme}}" aria-label="Switch light and dark theme"><svg class="ico" viewBox="0 0 24 24" aria-hidden="true">' + ICON['theme'] + '</svg><span>Theme</span></button>\n</nav>')
PAGES = {'main.html': ('Service-map.dc.html','map'), 'overview.html': ('Overview.dc.html','overview'), 'drift.html': ('Drift.dc.html','drift'), 'findings.html': ('Findings.dc.html','findings'), 'agents.html': ('Agents.dc.html','agents'), 'agentfirst.html': ('Agent-first.dc.html',None), 'foundations.html': ('Foundations.dc.html',None), 'start.html': ('Start-here.dc.html',None)}
for src,(out,cur) in PAGES.items():
    p = os.path.join(SRC, src)
    if not os.path.exists(p):
        print('skip', src); continue
    t = open(p).read()
    import re as _re
    head = open(os.path.join(SRC, '_head.html')).read()
    ask = open(os.path.join(SRC, '_askbar.html')).read()
    ins = open(os.path.join(SRC, '_insights.html')).read()
    t = _re.sub(r'@@HEAD:(.*?)@@', lambda m: head.replace('@@TITLE@@', m.group(1)), t)
    t = t.replace('@@ASKBAR@@', ask).replace('@@INSIGHTS@@', ins)
    t = t.replace('@@CSS@@', css).replace('@@DATA@@', data)
    if cur: t = t.replace('@@RAIL@@', rail(cur))
    else: t = t.replace('@@RAIL@@', '')
    leftover = re.findall(r'@@[A-Z]+@@', t)
    if leftover: print('WARN leftover placeholders in', src, leftover)
    open(os.path.join(OUT, out), 'w').write(t)
    print('built', out, len(t), 'bytes')
