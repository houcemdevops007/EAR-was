// Tests de applications/09-efi-calculator/.../calc-core.js (aucun eval utilisé par le moteur)
load(ROOT + '/applications/09-efi-calculator/web/src/main/webapp/assets/js/calc-core.js');
var K = CalcCore;

test('Quatre opérations et priorités', function () {
  eq(K.evaluate('2+3'), 5);
  eq(K.evaluate('10-4'), 6);
  eq(K.evaluate('6×7'), 42);
  eq(K.evaluate('84÷2'), 42);
  eq(K.evaluate('2+3*4'), 14);
  eq(K.evaluate('(2+3)*4'), 20);
  eq(K.evaluate('10-2-3'), 5);
  eq(K.evaluate('100/10/2'), 5);
});
test('Décimales françaises et précision', function () {
  eq(K.evaluate('0,1+0,2'), 0.3);
  eq(K.evaluate('1.5*2'), 3);
  eq(K.evaluate('1/3*3'), 1);
  eq(K.evaluate(',5+,5'), 1);
});
test('Nombres négatifs et signe unaire', function () {
  eq(K.evaluate('-5+2'), -3);
  eq(K.evaluate('3*-2'), -6);
  eq(K.evaluate('-(2+3)'), -5);
  eq(K.evaluate('2−3'), -1);
  eq(K.evaluate('--4'), 4);
});
test('Pourcentages façon calculatrice de bureau', function () {
  eq(K.evaluate('50%'), 0.5);
  eq(K.evaluate('200+10%'), 220);
  eq(K.evaluate('200-10%'), 180);
  eq(K.evaluate('200*10%'), 20);
  eq(K.evaluate('200/50%'), 400);
  eq(K.evaluate('(100+50)+20%'), 180);
});
test('Division par zéro et erreurs de syntaxe', function () {
  throws(function () { K.evaluate('5/0'); }, /Division par zéro/);
  throws(function () { K.evaluate('5/(2-2)'); }, /Division par zéro/);
  throws(function () { K.evaluate('2+'); }, /incomplète/);
  throws(function () { K.evaluate('(2+3'); }, /Parenthèse fermante manquante/);
  throws(function () { K.evaluate('2+3)'); }, /en trop/);
  throws(function () { K.evaluate('*2'); }, /mal placé/);
  throws(function () { K.evaluate('1,2,3'); }, /séparateur/);
  throws(function () { K.evaluate(''); }, /Saisissez/);
});
test('Refus de tout code : seuls chiffres et opérateurs sont acceptés', function () {
  throws(function () { K.evaluate('alert(1)'); }, /non autorisé/);
  throws(function () { K.evaluate('2;3'); }, /non autorisé/);
  throws(function () { K.evaluate('constructor'); }, /non autorisé/);
  throws(function () { K.evaluate(new Array(300).join('1')); }, /trop longue/);
});
test('Très grands nombres : résultat fini affiché en notation scientifique', function () {
  var expr = new Array(17).join('9999999999*') + '9999999999';
  ok(expr.length <= K.MAX_LENGTH, 'expression sous la limite');
  var r = K.evaluate(expr);
  ok(isFinite(r) && r > 1e169, 'résultat fini');
  ok(K.format(r).indexOf('10^') > 0, 'notation scientifique');
});
test('Aperçu tolérant et fermeture automatique', function () {
  eq(K.autoClose('(2+(3'), '(2+(3))');
  eq(K.tryEvaluate('(2+3'), 5);
  eq(K.tryEvaluate('2+'), null);
  eq(K.tryEvaluate('1/0'), null);
});
test('Formatage français', function () {
  eq(K.format(1234567.89), '1\u202f234\u202f567,89');
  eq(K.format(-42), '\u221242');
  eq(K.format(0.5), '0,5');
  eq(K.format(0), '0');
  eq(K.format(Infinity), '—');
  ok(K.format(1e20).indexOf('10^20') > 0);
});
test('Inversion du signe du dernier nombre', function () {
  eq(K.toggleSign('12+3'), '12+(-3)');
  eq(K.toggleSign('12+(-3)'), '12+3');
  eq(K.toggleSign('7'), '(-7)');
  eq(K.toggleSign('5+'), '5+');
  eq(K.pretty('2*3/4-1.5'), '2×3÷4−1,5');
});
done('calc-core');
