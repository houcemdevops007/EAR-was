/*
 * Mini-bibliothèque d'assertions pour Nashorn (jjs, JDK 8).
 * Variables globales : test(nom, fn), eq(a, b, msg), ok(v, msg), throws(fn, motif), ROOT, done().
 */
var ROOT = java.lang.System.getProperty('root');
var __results = { pass: 0, fail: 0, failures: [] };

function __fmt(v) { try { return JSON.stringify(v); } catch (e) { return String(v); } }

function eq(actual, expected, msg) {
  var a = __fmt(actual), e = __fmt(expected);
  if (a !== e) { throw new Error((msg ? msg + ' : ' : '') + 'attendu ' + e + ', obtenu ' + a); }
}
function ok(v, msg) { if (!v) { throw new Error(msg || 'condition fausse'); } }
function throws(fn, pattern, msg) {
  try { fn(); } catch (e) {
    if (pattern && !pattern.test(String(e.message))) { throw new Error((msg || 'exception') + ' : message inattendu « ' + e.message + ' »'); }
    return;
  }
  throw new Error(msg || 'une exception était attendue');
}
function test(name, fn) {
  try { fn(); __results.pass++; print('  \u2713 ' + name); }
  catch (e) { __results.fail++; __results.failures.push(name + ' — ' + e.message); print('  \u2717 ' + name + ' — ' + e.message); }
}
function done(suite) {
  print(suite + ' : ' + __results.pass + ' réussi(s), ' + __results.fail + ' échec(s)');
  if (__results.fail > 0) { exit(1); }
}
