/*
 * EFI Calculator — moteur de calcul pur, SANS eval() ni Function().
 * Analyseur syntaxique à descente récursive :
 *   expression := terme (('+' | '-') terme)*
 *   terme      := facteur (('×' | '÷') facteur)*
 *   facteur    := ('-' | '+') facteur | postfixe
 *   postfixe   := primaire ('%')*
 *   primaire   := nombre | '(' expression ')'
 * Sémantique des pourcentages (comme une calculatrice de bureau) :
 *   200 + 10 %  → 220     200 − 10 %  → 180     200 × 10 %  → 20     50 %  → 0,5
 * Syntaxe ES5 : testable avec Nashorn (jjs) — voir tests/js/test-calc-core.js.
 */
(function (root) {
  'use strict';

  var MAX_LENGTH = 200;

  function CalcError(message) { this.name = 'CalcError'; this.message = message; }
  CalcError.prototype = Object.create(Error.prototype);
  CalcError.prototype.constructor = CalcError;

  /** Découpe l'expression en jetons. Accepte « , » ou « . » décimal, × * x, ÷ /, − -. */
  function tokenize(src) {
    if (typeof src !== 'string') { throw new CalcError('Expression invalide.'); }
    if (src.length > MAX_LENGTH) { throw new CalcError('Expression trop longue (' + MAX_LENGTH + ' caractères maximum).'); }
    var tokens = [], i = 0;
    while (i < src.length) {
      var c = src.charAt(i);
      if (c === ' ' || c === '\u00a0' || c === '\u202f') { i++; continue; }
      if ((c >= '0' && c <= '9') || c === ',' || c === '.') {
        var start = i, seenDot = false;
        while (i < src.length) {
          var d = src.charAt(i);
          if (d >= '0' && d <= '9') { i++; }
          else if ((d === ',' || d === '.') && !seenDot) { seenDot = true; i++; }
          else if (d === ',' || d === '.') { throw new CalcError('Nombre mal formé : séparateur décimal en double.'); }
          else { break; }
        }
        var text = src.substring(start, i).replace(',', '.');
        if (text === '.') { throw new CalcError('Nombre mal formé.'); }
        tokens.push({ t: 'num', v: parseFloat(text) });
        continue;
      }
      if (c === '+') { tokens.push({ t: '+' }); }
      else if (c === '-' || c === '\u2212') { tokens.push({ t: '-' }); }
      else if (c === '*' || c === '\u00d7' || c === 'x' || c === 'X') { tokens.push({ t: '*' }); }
      else if (c === '/' || c === '\u00f7' || c === ':') { tokens.push({ t: '/' }); }
      else if (c === '%') { tokens.push({ t: '%' }); }
      else if (c === '(') { tokens.push({ t: '(' }); }
      else if (c === ')') { tokens.push({ t: ')' }); }
      else { throw new CalcError('Caractère non autorisé : « ' + c + ' ».'); }
      i++;
    }
    return tokens;
  }

  function Parser(tokens) { this.tk = tokens; this.pos = 0; }
  Parser.prototype.peek = function () { return this.tk[this.pos]; };
  Parser.prototype.next = function () { return this.tk[this.pos++]; };

  Parser.prototype.expression = function () {
    var left = this.term();
    var t = this.peek();
    while (t && (t.t === '+' || t.t === '-')) {
      this.next();
      var right = this.term();
      var rv = right.v;
      // « a + b % » : pourcentage de a
      if (right.pct) { rv = left.v * right.v; }
      left = { v: t.t === '+' ? left.v + rv : left.v - rv, pct: false };
      t = this.peek();
    }
    return left;
  };

  Parser.prototype.term = function () {
    var left = this.factor();
    var t = this.peek();
    while (t && (t.t === '*' || t.t === '/')) {
      this.next();
      var right = this.factor();
      if (t.t === '/') {
        if (right.v === 0) { throw new CalcError('Division par zéro impossible.'); }
        left = { v: left.v / right.v, pct: false };
      } else {
        left = { v: left.v * right.v, pct: false };
      }
      t = this.peek();
    }
    return left;
  };

  Parser.prototype.factor = function () {
    var t = this.peek();
    if (t && (t.t === '-' || t.t === '+')) {
      this.next();
      var f = this.factor();
      return { v: t.t === '-' ? -f.v : f.v, pct: f.pct };
    }
    return this.postfix();
  };

  Parser.prototype.postfix = function () {
    var p = this.primary();
    while (this.peek() && this.peek().t === '%') {
      this.next();
      p = { v: p.v / 100, pct: true };
    }
    return p;
  };

  Parser.prototype.primary = function () {
    var t = this.next();
    if (!t) { throw new CalcError('Expression incomplète.'); }
    if (t.t === 'num') { return { v: t.v, pct: false }; }
    if (t.t === '(') {
      var e = this.expression();
      var close = this.next();
      if (!close || close.t !== ')') { throw new CalcError('Parenthèse fermante manquante.'); }
      return { v: e.v, pct: false };
    }
    if (t.t === ')') { throw new CalcError('Parenthèse fermante inattendue.'); }
    throw new CalcError('Opérateur mal placé.');
  };

  /** Arrondit à 12 chiffres significatifs pour masquer les artefacts binaires (0,1 + 0,2). */
  function tidy(n) {
    if (n === 0) { return 0; }
    return parseFloat(n.toPrecision(12));
  }

  /** Évalue une expression ; lève CalcError en cas d'erreur. */
  function evaluate(src) {
    var tokens = tokenize(src);
    if (!tokens.length) { throw new CalcError('Saisissez un calcul.'); }
    var p = new Parser(tokens);
    var r = p.expression();
    if (p.pos < tokens.length) {
      var extra = tokens[p.pos];
      throw new CalcError(extra.t === ')' ? 'Parenthèse fermante en trop.' : 'Expression invalide.');
    }
    if (!isFinite(r.v)) { throw new CalcError('Résultat hors limites.'); }
    return tidy(r.v);
  }

  /** Évaluation tolérante pour l'aperçu en direct (null si incomplet ou invalide). */
  function tryEvaluate(src) {
    try { return evaluate(autoClose(src)); } catch (e) { return null; }
  }

  /** Ajoute les parenthèses fermantes manquantes (« (2+3 » → « (2+3) »). */
  function autoClose(src) {
    var open = 0;
    for (var i = 0; i < src.length; i++) {
      if (src.charAt(i) === '(') { open++; } else if (src.charAt(i) === ')') { open--; }
    }
    while (open-- > 0) { src += ')'; }
    return src;
  }

  /** Formate un nombre à la française : 1 234 567,89 (espace fine insécable). */
  function format(n) {
    if (typeof n !== 'number' || !isFinite(n)) { return '—'; }
    var abs = Math.abs(n);
    if (abs !== 0 && (abs >= 1e15 || abs < 1e-9)) {
      return n.toExponential(8).replace(/\.?0+e/, 'e').replace('.', ',').replace('e+', ' × 10^').replace('e-', ' × 10^-');
    }
    var s = String(tidy(abs));
    if (s.indexOf('e') >= 0) { s = abs.toFixed(10).replace(/0+$/, '').replace(/\.$/, ''); }
    var parts = s.split('.');
    var digits = parts[0], int = '';
    for (var i = 0; i < digits.length; i++) {
      if (i > 0 && (digits.length - i) % 3 === 0) { int += '\u202f'; }
      int += digits.charAt(i);
    }
    return (n < 0 ? '\u2212' : '') + int + (parts[1] ? ',' + parts[1] : '');
  }

  /** Remplace les opérateurs ASCII par leur forme typographique pour l'affichage. */
  function pretty(src) {
    return src.replace(/\*/g, '×').replace(/\//g, '÷').replace(/-/g, '−').replace(/\./g, ',');
  }

  /** Inverse le signe du dernier nombre saisi : « 12+3 » → « 12+(-3) » et inversement. */
  function toggleSign(src) {
    var m = /(\(-)?(\d+[.,]?\d*)(\))?$/.exec(src);
    if (!m) { return src; }
    var before = src.substring(0, m.index);
    if (m[1] && m[3]) { return before + m[2]; }
    if (m[1] && !m[3]) { return before + m[2]; }
    return before + '(-' + m[2] + ')';
  }

  root.CalcCore = { CalcError: CalcError, tokenize: tokenize, evaluate: evaluate, tryEvaluate: tryEvaluate,
    autoClose: autoClose, format: format, pretty: pretty, toggleSign: toggleSign, MAX_LENGTH: MAX_LENGTH };
})(this);
