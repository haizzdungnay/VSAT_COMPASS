import React from 'react';
import katex from 'katex';

interface MathViewProps {
  content?: string;
  className?: string;
}

export const MathView: React.FC<MathViewProps> = ({ content = '', className = '' }) => {
  if (!content) return null;

  // Split by $$...$$, $...$, \[...], \(...\)
  const mathRegex = /(\$\$[\s\S]*?\$\$|\$[^$]+?\$|\\\[[\s\S]*?\\\]|\\\([\s\S]*?\\\))/g;
  const parts = content.split(mathRegex);

  return (
    <span className={className}>
      {parts.map((part, idx) => {
        if (!part) return null;
        if (part.startsWith('$$') && part.endsWith('$$')) {
          const formula = part.slice(2, -2).trim();
          try {
            const html = katex.renderToString(formula, { displayMode: true, throwOnError: false });
            return <span key={idx} dangerouslySetInnerHTML={{ __html: html }} className="my-1 block" />;
          } catch {
            return <span key={idx} className="font-mono text-amber-600">{part}</span>;
          }
        }
        if (part.startsWith('\\[') && part.endsWith('\\]')) {
          const formula = part.slice(2, -2).trim();
          try {
            const html = katex.renderToString(formula, { displayMode: true, throwOnError: false });
            return <span key={idx} dangerouslySetInnerHTML={{ __html: html }} className="my-1 block" />;
          } catch {
            return <span key={idx} className="font-mono text-amber-600">{part}</span>;
          }
        }
        if (part.startsWith('$') && part.endsWith('$')) {
          const formula = part.slice(1, -1).trim();
          try {
            const html = katex.renderToString(formula, { displayMode: false, throwOnError: false });
            return <span key={idx} dangerouslySetInnerHTML={{ __html: html }} />;
          } catch {
            return <span key={idx} className="font-mono text-amber-600">{part}</span>;
          }
        }
        if (part.startsWith('\\(') && part.endsWith('\\)')) {
          const formula = part.slice(2, -2).trim();
          try {
            const html = katex.renderToString(formula, { displayMode: false, throwOnError: false });
            return <span key={idx} dangerouslySetInnerHTML={{ __html: html }} />;
          } catch {
            return <span key={idx} className="font-mono text-amber-600">{part}</span>;
          }
        }
        return <span key={idx}>{part}</span>;
      })}
    </span>
  );
};
