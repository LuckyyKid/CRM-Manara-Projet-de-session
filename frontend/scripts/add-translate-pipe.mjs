import fs from 'node:fs';
import path from 'node:path';

const appDir = path.join(process.cwd(), 'src', 'app');

for (const filePath of walk(appDir)) {
  if (!filePath.endsWith('.ts')) {
    continue;
  }

  const htmlPath = filePath.replace(/\.ts$/, '.html');
  if (!fs.existsSync(htmlPath)) {
    continue;
  }

  const html = fs.readFileSync(htmlPath, 'utf8');
  if (!html.includes('| translate')) {
    continue;
  }

  let source = fs.readFileSync(filePath, 'utf8');
  if (!source.includes('templateUrl:') || !source.includes('imports: [')) {
    continue;
  }

  if (!source.includes("from '@ngx-translate/core'")) {
    source = `import { TranslatePipe } from '@ngx-translate/core';\n${source}`;
  } else if (!source.includes('TranslatePipe')) {
    source = source.replace(
      /import\s*\{([^}]+)\}\s*from '@ngx-translate\/core';/,
      (_, imports) => `import { ${imports.trim()}, TranslatePipe } from '@ngx-translate/core';`,
    );
  }

  source = source.replace(/imports:\s*\[([\s\S]*?)\]/m, (full, block) => {
    if (block.includes('TranslatePipe')) {
      return full;
    }
    const content = block.trim();
    return `imports: [${content ? `${content}, TranslatePipe` : 'TranslatePipe'}]`;
  });

  fs.writeFileSync(filePath, source, 'utf8');
}

function walk(directory) {
  const files = [];
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    const fullPath = path.join(directory, entry.name);
    if (entry.isDirectory()) {
      files.push(...walk(fullPath));
    } else {
      files.push(fullPath);
    }
  }
  return files;
}
