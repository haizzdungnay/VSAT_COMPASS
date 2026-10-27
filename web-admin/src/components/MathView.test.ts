// Self-check for KaTeX math engine
import katex from 'katex';

export function runKaTeXSelfCheck(): boolean {
  const h1 = katex.renderToString('f(x) = x^2 + 2x + 1', { throwOnError: false });
  if (!h1.includes('katex')) {
    throw new Error('KaTeX rendering assertion failed');
  }

  const h2 = katex.renderToString('\\int_{0}^{1} e^x dx', { throwOnError: false });
  if (!h2.includes('katex')) {
    throw new Error('KaTeX integral rendering assertion failed');
  }

  return true;
}

// ponytail: basic assert self-check without test framework overhead. Add Vitest when suite > 20 tests.
