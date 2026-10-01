const W = 192, H = 56;
const ENVS = ['DEV', 'STAGE', 'PROD'];
const ICONS = {
  service: 'M4 6.5h16v4.5H4zM4 13h16v4.5H4zM7.5 8.75h.01M7.5 15.25h.01',
  database: 'M4 6c0-1.7 3.6-3 8-3s8 1.3 8 3-3.6 3-8 3-8-1.3-8-3zM4 6v12c0 1.7 3.6 3 8 3s8-1.3 8-3V6M4 12c0 1.7 3.6 3 8 3s8-1.3 8-3',
  topic: 'M4 11a9 9 0 0 1 9 9M4 4a16 16 0 0 1 16 16M5 19h.01',
  external: 'M12 3a9 9 0 1 0 0 18 9 9 0 1 0 0-18zM3 12h18M12 3c3 3 3 15 0 18M12 3c-3 3-3 15 0 18'
};
const TCLS = { service: 't-svc', database: 't-db', topic: 't-topic', external: 't-ext' };
const TYPE_LABEL = { service: 'Services', database: 'Databases', topic: 'Topics', external: 'External' };
const COLORS = {
  dark: { grey: '#4E5D76', amber: '#F5A524', red: '#FF6B6B', accent: '#38BDF8', added: '#3DD68C', removed: '#FF6B6B', changed: '#F5A524', service: '#8B9CFF', database: '#F2B33D', topic: '#F28AC1', external: '#9AA7B8' },
  light: { grey: '#94A0B4', amber: '#B7791F', red: '#C53030', accent: '#0369A1', added: '#1E9E5E', removed: '#C53030', changed: '#B7791F', service: '#4F63D2', database: '#B7791F', topic: '#C2418F', external: '#5F6B80' }
};
const NODES = [
  { key: 'gateway', id: 'service:edge/api-gateway', type: 'service', name: 'api-gateway', domain: 'edge', x: 36, y: 370, envs: ENVS, ver: { DEV: '3.4.0', STAGE: '3.4.0', PROD: '3.4.0' }, pods: { DEV: 1, STAGE: 2, PROD: 3 }, history: [{ v: '3.4.0', since: '22 Sep, 09:14' }, { v: '3.3.2', since: '08 Sep, 16:40' }] },
  { key: 'orders', id: 'service:orders/orders-service', type: 'service', name: 'orders-service', domain: 'orders', x: 276, y: 96, envs: ENVS, ver: { DEV: '2.8.1', STAGE: '2.8.1', PROD: '2.8.1' }, pods: { DEV: 1, STAGE: 2, PROD: 3 }, history: [{ v: '2.8.1', since: 'Today, 14:10' }, { v: '2.8.0', since: '25 Sep, 11:02' }] },
  { key: 'inventory', id: 'service:orders/inventory-service', type: 'service', name: 'inventory-service', domain: 'orders', x: 276, y: 188, envs: ENVS, ver: { DEV: '1.9.0', STAGE: '1.9.0', PROD: '1.9.0' }, pods: { DEV: 1, STAGE: 1, PROD: 2 }, history: [{ v: '1.9.0', since: '18 Sep, 10:30' }] },
  { key: 'payments', id: 'service:payments/payments-service', type: 'service', name: 'payments-service', domain: 'payments', x: 276, y: 324, envs: ENVS, ver: { DEV: '4.2.0-rc.1', STAGE: '4.1.2', PROD: '4.1.2' }, pods: { DEV: 1, STAGE: 2, PROD: 2 }, history: [{ v: '4.1.2', since: '29 Sep, 17:55' }, { v: '4.1.1', since: '15 Sep, 12:20' }] },
  { key: 'catalog', id: 'service:catalog/catalog-service', type: 'service', name: 'catalog-service', domain: 'catalog', x: 276, y: 416, envs: ENVS, ver: { DEV: '2.2.1', STAGE: '2.2.1', PROD: '2.2.1' }, pods: { DEV: 1, STAGE: 2, PROD: 4 }, history: [{ v: '2.2.1', since: '12 Sep, 08:05' }] },
  { key: 'search', id: 'service:catalog/search-service', type: 'service', name: 'search-service', domain: 'catalog', x: 276, y: 508, envs: ENVS, ver: { DEV: '2.3.0', STAGE: '2.2.1', PROD: '2.2.1' }, pods: { DEV: 1, STAGE: 2, PROD: 2 }, history: [{ v: '2.2.1', since: '12 Sep, 08:05' }] },
  { key: 'redis', id: 'db:redis/sessions', type: 'database', name: 'sessions', domain: 'identity', x: 276, y: 600, envs: ENVS, sub: 'redis' },
  { key: 'dbOrders', id: 'db:postgresql/orders', type: 'database', name: 'orders', domain: 'orders', x: 516, y: 52, envs: ENVS, sub: 'postgresql' },
  { key: 'topicOrders', id: 'topic:kafka/order-events', type: 'topic', name: 'order-events', domain: 'orders', x: 516, y: 144, envs: ENVS, sub: 'kafka' },
  { key: 'dbPay', id: 'db:postgresql/payments', type: 'database', name: 'payments', domain: 'payments', x: 516, y: 280, envs: ENVS, sub: 'postgresql' },
  { key: 'topicPay', id: 'topic:kafka/payment-events', type: 'topic', name: 'payment-events', domain: 'payments', x: 516, y: 372, envs: ENVS, sub: 'kafka' },
  { key: 'extStripe', id: 'ext:api.stripe.com', type: 'external', name: 'api.stripe.com', domain: 'payments', x: 516, y: 464, envs: ENVS, sub: 'https' },
  { key: 'dbCatalog', id: 'db:postgresql/catalog', type: 'database', name: 'catalog', domain: 'catalog', x: 516, y: 556, envs: ENVS, sub: 'postgresql' },
  { key: 'reporting', id: 'service:platform/reporting-service', type: 'service', name: 'reporting-service', domain: 'platform', x: 756, y: 52, envs: ['PROD'], ver: { PROD: '0.9.3' }, pods: { PROD: 1 }, history: [{ v: '0.9.3', since: 'Yesterday, 18:05' }] },
  { key: 'notify', id: 'service:platform/notification-service', type: 'service', name: 'notification-service', domain: 'platform', x: 756, y: 280, envs: ENVS, ver: { DEV: '1.4.0', STAGE: '1.4.0', PROD: '1.4.0' }, pods: { DEV: 1, STAGE: 1, PROD: 2 }, history: [{ v: '1.4.0', since: '20 Sep, 13:00' }] },
  { key: 'extSendgrid', id: 'ext:smtp.sendgrid.net', type: 'external', name: 'smtp.sendgrid.net', domain: 'platform', x: 756, y: 372, envs: ENVS, sub: 'smtp' }
];
const EDGES = [
  { from: 'gateway', to: 'orders', kind: 'sync', calls: 12400, err: 0.3, p50: 38, p95: 142, p99: 310, max: 1200 },
  { from: 'gateway', to: 'payments', kind: 'sync', calls: 2100, err: 0.8, p50: 120, p95: 310, p99: 640, max: 2400 },
  { from: 'gateway', to: 'catalog', kind: 'sync', calls: 31800, err: 0.1, p50: 12, p95: 48, p99: 95, max: 410 },
  { from: 'gateway', to: 'search', kind: 'sync', calls: 9600, err: 0.0, p50: 30, p95: 88, p99: 160, max: 620 },
  { from: 'gateway', to: 'redis', kind: 'sync', calls: 40200, err: 0.0, p50: 1, p95: 3, p99: 6, max: 40 },
  { from: 'orders', to: 'inventory', kind: 'sync', calls: 8900, err: 0.2, p50: 18, p95: 61, p99: 120, max: 480, ox: 64 },
  { from: 'inventory', to: 'orders', kind: 'sync', calls: 1200, err: 0.0, p50: 14, p95: 35, p99: 70, max: 210, ox: 128 },
  { from: 'orders', to: 'dbOrders', kind: 'sync', calls: 24000, err: 0.0, p50: 3, p95: 9, p99: 22, max: 140 },
  { from: 'orders', to: 'topicOrders', kind: 'publish', calls: 8700, err: 0.0, p50: 2, p95: 5, p99: 9, max: 60 },
  { from: 'payments', to: 'dbPay', kind: 'sync', calls: 4000, err: 0.0, p50: 3, p95: 8, p99: 18, max: 90 },
  { from: 'payments', to: 'extStripe', kind: 'sync', calls: 2000, err: 1.9, p50: 210, p95: 640, p99: 1300, max: 4100 },
  { from: 'payments', to: 'topicPay', kind: 'publish', calls: 2000, err: 0.0, p50: 2, p95: 6, p99: 11, max: 70 },
  { from: 'catalog', to: 'dbCatalog', kind: 'sync', calls: 30000, err: 0.0, p50: 2, p95: 7, p99: 15, max: 120 },
  { from: 'search', to: 'catalog', kind: 'sync', calls: 5200, err: 0.0, p50: 9, p95: 28, p99: 60, max: 300 },
  { from: 'search', to: 'dbCatalog', kind: 'sync', calls: 1800, err: 0.0, p50: 4, p95: 12, p99: 30, max: 160, envs: ['DEV'], since: 'today' },
  { from: 'topicOrders', to: 'notify', kind: 'consume', calls: 8700, err: 0.0, p50: 4, p95: 12, p99: 30, max: 180 },
  { from: 'topicPay', to: 'notify', kind: 'consume', calls: 2000, err: 0.0, p50: 4, p95: 11, p99: 28, max: 150 },
  { from: 'notify', to: 'extSendgrid', kind: 'sync', calls: 6100, err: 4.2, p50: 260, p95: 820, p99: 1900, max: 5200 },
  { from: 'topicOrders', to: 'reporting', kind: 'consume', calls: 8700, err: 0.0, p50: 6, p95: 20, p99: 45, max: 200, envs: ['PROD'], since: 'yesterday' },
  { from: 'reporting', to: 'dbOrders', kind: 'sync', calls: 420, err: 0.0, p50: 60, p95: 180, p99: 400, max: 1500, envs: ['PROD'], since: 'yesterday' }
];
const RULES = {
  CyclicDependency: { label: 'Cyclic dependency', threshold: 'any cycle over sync edges', config: 'architrace.rules.cyclic-dependency.enabled=true' },
  SharedDatabase: { label: 'Shared database', threshold: 'more than 1 service per database', config: 'architrace.rules.shared-database.max-services=1' },
  CrossDomainCoupling: { label: 'Cross-domain coupling', threshold: 'sync edges into more than 3 other domains', config: 'architrace.rules.cross-domain-coupling.max-domains=3' },
  FanInHub: { label: 'Fan-in hub', threshold: 'inbound sync degree above 8', config: 'architrace.rules.fan-in-hub.max-inbound=8' },
  LongSyncChain: { label: 'Long sync chain', threshold: 'sync path longer than 5 hops', config: 'architrace.rules.long-sync-chain.max-hops=5' },
  UnknownExternal: { label: 'Unknown external', threshold: 'external not in the allowlist', config: 'architrace.rules.unknown-external.allowlist=' }
};
const FINDINGS = [
  { id: 'f1', rule: 'CyclicDependency', sev: 'high', envs: ENVS, subjects: ['orders', 'inventory'], title: 'orders-service and inventory-service call each other synchronously', detail: 'A strongly connected component over sync edges. A latency spike or outage in either service feeds back into the other.', evidence: ['orders-service → inventory-service · sync · 8.9k calls · p95 61 ms', 'inventory-service → orders-service · sync · 1.2k calls · p95 35 ms'] },
  { id: 'f2', rule: 'SharedDatabase', sev: 'high', envs: ['PROD'], subjects: ['dbOrders', 'orders', 'reporting'], title: 'postgresql/orders is used by 2 services', detail: 'orders-service owns the schema; reporting-service reads it directly. A schema change in one breaks the other without any contract in between.', evidence: ['orders-service → postgresql/orders · sync · 24.0k calls', 'reporting-service → postgresql/orders · sync · 420 calls · first seen yesterday 18:05'] },
  { id: 'f6', rule: 'SharedDatabase', sev: 'high', envs: ['DEV'], subjects: ['dbCatalog', 'catalog', 'search'], title: 'postgresql/catalog is used by 2 services', detail: 'search-service 2.3.0 reads the catalog database directly instead of calling catalog-service. Promoting this version to PROD will raise the same finding there.', evidence: ['catalog-service → postgresql/catalog · sync · 30.0k calls', 'search-service → postgresql/catalog · sync · 1.8k calls · first seen today 11:42'] },
  { id: 'f3', rule: 'CrossDomainCoupling', sev: 'medium', envs: ENVS, subjects: ['gateway'], title: 'api-gateway calls into 4 other domains', detail: 'Threshold is 3. An edge gateway is expected to fan out; consider raising the threshold for the edge domain rather than ignoring the finding.', evidence: ['orders: orders-service', 'payments: payments-service', 'catalog: catalog-service, search-service', 'identity: redis/sessions'] },
  { id: 'f4', rule: 'UnknownExternal', sev: 'low', envs: ENVS, subjects: ['extStripe'], title: 'api.stripe.com is not in the external allowlist', detail: 'Called by payments-service. Add the host to the allowlist if the dependency is intended.', evidence: ['payments-service → api.stripe.com · sync · 2.0k calls · 1.9% errors'], allow: 'architrace.rules.unknown-external.allowlist=api.stripe.com' },
  { id: 'f5', rule: 'UnknownExternal', sev: 'low', envs: ENVS, subjects: ['extSendgrid'], title: 'smtp.sendgrid.net is not in the external allowlist', detail: 'Called by notification-service. Add the host to the allowlist if the dependency is intended.', evidence: ['notification-service → smtp.sendgrid.net · sync · 6.1k calls · 4.2% errors'], allow: 'architrace.rules.unknown-external.allowlist=smtp.sendgrid.net' }
];
const AGENTS = [
  { name: 'prod-eu1-a', env: 'PROD', cluster: 'k8s-prod-eu1', status: 'live', last: '42 s ago', window: '60 s', rate: '3.2k/s', dropped: '0', version: '0.4.0', services: 5 },
  { name: 'prod-eu1-b', env: 'PROD', cluster: 'k8s-prod-eu1', status: 'live', last: '38 s ago', window: '60 s', rate: '2.9k/s', dropped: '0', version: '0.4.0', services: 4 },
  { name: 'prod-eu2', env: 'PROD', cluster: 'k8s-prod-eu2', status: 'live', last: '51 s ago', window: '60 s', rate: '1.1k/s', dropped: '0', version: '0.4.0', services: 3 },
  { name: 'stage-eu1', env: 'STAGE', cluster: 'k8s-stage-eu1', status: 'live', last: '12 s ago', window: '60 s', rate: '640/s', dropped: '0', version: '0.4.0', services: 7 },
  { name: 'dev-eu1', env: 'DEV', cluster: 'k8s-dev-eu1', status: 'live', last: '55 s ago', window: '60 s', rate: '210/s', dropped: '0', version: '0.4.0', services: 7 },
  { name: 'dev-ci', env: 'DEV', cluster: 'k8s-dev-ci', status: 'stale', last: '2 h ago', window: '60 s', rate: '0/s', dropped: '0', version: '0.3.9', services: 0 }
];
const byKey = Object.fromEntries(NODES.map((n) => [n.key, n]));
const edgeId = (e) => e.from + '>' + e.to + ':' + e.kind;
function fmtK(n) { return n >= 1000 ? (n / 1000).toFixed(1).replace(/\.0$/, '') + 'k' : String(n); }
function clusterOf(env) { return 'k8s-' + env.toLowerCase() + '-eu1'; }
function edgeGeom(a, b, e) {
  if (a.x === b.x) {
    const x = a.x + (e.ox ?? W / 2);
    const down = b.y > a.y;
    const y1 = down ? a.y + H : a.y;
    const y2 = down ? b.y : b.y + H;
    const side = e.ox && e.ox < W / 2 ? -44 : 44;
    return { d: 'M ' + x + ' ' + y1 + ' L ' + x + ' ' + y2, lx: x + side, ly: (y1 + y2) / 2 };
  }
  if (b.x < a.x) {
    const x1 = a.x, y1 = a.y + H / 2, x2 = b.x + W, y2 = b.y + H / 2;
    return { d: 'M ' + x1 + ' ' + y1 + ' C ' + (x1 - 34) + ' ' + y1 + ', ' + (x2 + 34) + ' ' + y2 + ', ' + x2 + ' ' + y2, lx: (x1 + x2) / 2, ly: (y1 + y2) / 2 - 18 };
  }
  const x1 = a.x + W, y1 = a.y + H / 2, x2 = b.x, y2 = b.y + H / 2;
  return { d: 'M ' + x1 + ' ' + y1 + ' C ' + (x1 + 34) + ' ' + y1 + ', ' + (x2 - 34) + ' ' + y2 + ', ' + x2 + ' ' + y2, lx: (x1 + x2) / 2, ly: (y1 + y2) / 2 };
}
function graphAt(env, when) {
  const yesterday = when === 'yesterday';
  const nodes = NODES.filter((n) => n.envs.includes(env)).filter((n) => !(yesterday && n.since === 'yesterday' && env === 'PROD') && !(yesterday && n.key === 'reporting'))
    .map((n) => {
      let ver = n.ver ? n.ver[env] : null;
      if (yesterday) {
        if (env === 'PROD' && n.key === 'orders') ver = '2.8.0';
        if (env === 'DEV' && n.key === 'search') ver = '2.2.1';
        if (env === 'DEV' && n.key === 'payments') ver = '4.1.2';
      }
      return { node: n, ver };
    });
  const keys = new Set(nodes.map((i) => i.node.key));
  const edges = EDGES.filter((e) => (e.envs || ENVS).includes(env)).filter((e) => keys.has(e.from) && keys.has(e.to))
    .filter((e) => !(yesterday && e.since && ((env === 'PROD' && e.since === 'yesterday') || (env === 'DEV' && e.since === 'today'))))
    .map((e) => ({ edge: e }));
  return { nodes, edges };
}
function diffGraphs(L, R) {
  const lN = new Map(L.nodes.map((i) => [i.node.key, i])), rN = new Map(R.nodes.map((i) => [i.node.key, i]));
  const lE = new Map(L.edges.map((i) => [edgeId(i.edge), i])), rE = new Map(R.edges.map((i) => [edgeId(i.edge), i]));
  const nodesAdded = [...rN.values()].filter((i) => !lN.has(i.node.key));
  const nodesRemoved = [...lN.values()].filter((i) => !rN.has(i.node.key));
  const nodesChanged = [...rN.values()].filter((i) => lN.has(i.node.key) && lN.get(i.node.key).ver !== i.ver).map((i) => ({ node: i.node, before: lN.get(i.node.key).ver, after: i.ver }));
  const edgesAdded = [...rE.values()].filter((i) => !lE.has(edgeId(i.edge)));
  const edgesRemoved = [...lE.values()].filter((i) => !rE.has(edgeId(i.edge)));
  return { nodesAdded, nodesRemoved, nodesChanged, edgesAdded, edgesRemoved };
}
function findingsFor(env) { return FINDINGS.filter((f) => f.envs.includes(env)); }
function sevCounts(list) { return { high: list.filter((f) => f.sev === 'high').length, medium: list.filter((f) => f.sev === 'medium').length, low: list.filter((f) => f.sev === 'low').length }; }
function nodeFindings(env, key) { return findingsFor(env).filter((f) => f.subjects.includes(key)); }
function sevRank(s) { return s === 'high' ? 3 : s === 'medium' ? 2 : 1; }
function renderGraph(o) {
  const C = o.C;
  const items = o.items.filter((i) => o.types[i.node.type]);
  let visible = new Set(items.map((i) => i.node.key));
  if (o.domain !== 'all') {
    const svc = new Set(items.filter((i) => i.node.type === 'service' && i.node.domain === o.domain).map((i) => i.node.key));
    const attached = new Set();
    o.edgeItems.forEach((ei) => { if (svc.has(ei.edge.from)) attached.add(ei.edge.to); if (svc.has(ei.edge.to)) attached.add(ei.edge.from); });
    visible = new Set([...visible].filter((k) => byKey[k].type === 'service' ? svc.has(k) : attached.has(k)));
  }
  const q = (o.find || '').trim().toLowerCase();
  const sel = o.sel;
  const selKey = sel && sel.kind === 'node' ? sel.key : null;
  const selEdge = sel && sel.kind === 'edge' ? sel.id : null;
  const nodes = items.filter((i) => visible.has(i.node.key)).map((i) => {
    const n = i.node;
    const hit = q && (n.name.toLowerCase().includes(q) || n.domain.includes(q) || n.id.includes(q));
    const fl = nodeFindings(o.env, n.key);
    const top = fl.reduce((m, f) => (sevRank(f.sev) > sevRank(m) ? f.sev : m), 'low');
    const state = i.state || '';
    let op = 1;
    if (q) op = hit ? 1 : 0.28;
    else if (selKey) op = n.key === selKey || o.edgeItems.some((ei) => (ei.edge.from === selKey && ei.edge.to === n.key) || (ei.edge.to === selKey && ei.edge.from === n.key)) ? 1 : 0.4;
    return {
      key: n.key, name: n.name, x: n.x, y: n.y, icon: ICONS[n.type], tcls: TCLS[n.type], op,
      sub: i.sub ?? (n.type === 'service' ? 'v' + (i.ver || '') : n.sub),
      cls: (hit ? 'hit ' : '') + (state ? 'n-' + state : ''),
      selected: n.key === selKey,
      pick: () => o.pick({ kind: 'node', key: n.key }),
      hasFindings: fl.length > 0 && !state, findings: fl.length, badgeCls: 'sev-' + top,
      hasFlag: !!state, flag: state === 'added' ? '+' : state === 'removed' ? '−' : 'Δ', flagCls: 'b-' + state
    };
  });
  const edges = o.edgeItems.filter((ei) => visible.has(ei.edge.from) && visible.has(ei.edge.to)).map((ei) => {
    const e = ei.edge, a = byKey[e.from], b = byKey[e.to];
    const g = edgeGeom(a, b, e);
    const id = edgeId(e);
    const touching = selKey && (e.from === selKey || e.to === selKey);
    const isSel = selEdge === id;
    let tone = e.err >= 3 ? 'red' : e.err >= 1 ? 'amber' : 'grey';
    if (ei.state) tone = ei.state;
    if (touching || isSel) tone = 'accent';
    let op = 1;
    if (selKey || selEdge) op = touching || isSel ? 1 : 0.22;
    else if (q) op = 0.25;
    else if (ei.state === 'removed') op = 0.85;
    const w = Math.min(4, 1.2 + Math.log10(e.calls / 400 + 1)).toFixed(1);
    return { id, d: g.d, stroke: C[tone], w, dash: e.kind === 'sync' ? 'none' : '7 5', op, mid: 'ah-' + tone, marker: 'url(#ah-' + tone + ')', lx: g.lx, ly: g.ly, touching: !!touching || isSel, label: fmtK(e.calls) + ' · ' + e.err.toFixed(1) + '%', pick: () => o.pick({ kind: 'edge', id }), selected: isSel };
  });
  const pills = edges.filter((e) => e.touching).map((e) => ({ x: e.lx, y: e.ly, label: e.label, pick: e.pick, selected: e.selected }));
  const mini = nodes.map((n) => ({ x: Math.round(n.x * 0.19) + 6, y: Math.round(n.y * 0.19) + 10, w: Math.round(W * 0.19), h: Math.round(H * 0.19), c: C[byKey[n.key].type], op: n.op }));
  const counts = {};
  Object.keys(TYPE_LABEL).forEach((t) => { counts[t] = o.items.filter((i) => i.node.type === t).length; });
  return { nodes, edges, pills, mini, counts, visibleCount: nodes.length, edgeCount: edges.length };
}
const ACTIONS = [
  { label: 'Compare DEV with PROD', hint: 'environment drift', href: 'Drift.dc.html', kw: 'compare diff drift dev prod difference' },
  { label: 'What changed in PROD since yesterday', hint: 'release drift', href: 'Drift.dc.html', kw: 'change changed timeline release yesterday prod history' },
  { label: 'Show findings in PROD', hint: 'architecture rules', href: 'Findings.dc.html', kw: 'findings cycle cycles shared database rules violations problems' },
  { label: 'Open environments overview', hint: 'all environments', href: 'Overview.dc.html', kw: 'overview environments home summary' },
  { label: 'Check agents and liveness', hint: 'agents', href: 'Agents.dc.html', kw: 'agents agent liveness health snapshots stale' },
  { label: 'Open the service map', hint: 'PROD', href: 'Service-map.dc.html', kw: 'map service graph topology' }
];
function palette(query, env, pickNode) {
  const q = (query || '').trim().toLowerCase();
  const actions = ACTIONS.filter((a) => !q || a.label.toLowerCase().includes(q) || a.kw.includes(q)).slice(0, q ? 4 : 6);
  const nodes = q ? NODES.filter((n) => n.envs.includes(env) && (n.name.toLowerCase().includes(q) || n.domain.includes(q) || n.id.includes(q))).slice(0, 6).map((n) => ({ key: n.key, name: n.name, sub: n.id, icon: ICONS[n.type], tcls: TCLS[n.type], pick: () => pickNode(n.key) })) : [];
  return { actions, nodes, hasActions: actions.length > 0, hasNodes: nodes.length > 0, noMatch: q && actions.length === 0 && nodes.length === 0, query: query || '' };
}
