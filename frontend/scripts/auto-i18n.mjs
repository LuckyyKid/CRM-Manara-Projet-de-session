import fs from 'node:fs';
import path from 'node:path';

const rootDir = process.cwd();
const appDir = path.join(rootDir, 'src', 'app');
const frPath = path.join(rootDir, 'src', 'assets', 'i18n', 'fr.json');
const enPath = path.join(rootDir, 'src', 'assets', 'i18n', 'en.json');
const legacyI18nPath = path.join(rootDir, 'src', 'app', 'core', 'i18n', 'legacy-i18n.service.ts');

const frTranslations = readJson(frPath);
const enTranslations = readJson(enPath);
const legacyTranslations = loadLegacyTranslations(legacyI18nPath);
const frFlat = flattenObject(frTranslations);
const enFlat = flattenObject(enTranslations);
const existingFrToKey = new Map(Object.entries(frFlat).map(([key, value]) => [value, key]));
const allHtmlFiles = walk(appDir, (file) => file.endsWith('.html'));
const touchedComponentTs = new Set();

for (const filePath of allHtmlFiles) {
  const original = fs.readFileSync(filePath, 'utf8');
  let content = original;
  const fileNamespace = buildFileNamespace(filePath);
  const textCounts = new Map();

  content = content.replace(/\s(placeholder|title|aria-label|alt)="([^"]+)"/g, (fullMatch, attr, rawValue) => {
    if (shouldSkipAttribute(attr, rawValue)) {
      return fullMatch;
    }

    const key = ensureTranslation(fileNamespace, rawValue, textCounts);
    return ` ${attr}="{{ '${key}' | translate }}"`;
  });

  content = content.replace(/>([^<]+)</g, (fullMatch, rawText) => {
    if (shouldSkipText(rawText)) {
      return fullMatch;
    }

    const key = ensureTranslation(fileNamespace, rawText, textCounts);
    const leading = rawText.match(/^\s*/)?.[0] ?? '';
    const trailing = rawText.match(/\s*$/)?.[0] ?? '';
    return `>${leading}{{ '${key}' | translate }}${trailing}<`;
  });

  if (content !== original) {
    fs.writeFileSync(filePath, content, 'utf8');
    const siblingComponentTs = filePath.replace(/\.html$/, '.ts');
    if (fs.existsSync(siblingComponentTs)) {
      touchedComponentTs.add(siblingComponentTs);
    }
  }
}

const allComponentTs = walk(appDir, (file) => file.endsWith('.ts'));
for (const componentPath of new Set([...touchedComponentTs, ...allComponentTs])) {
  const siblingHtml = componentPath.endsWith('.ts') ? componentPath.replace(/\.ts$/, '.html') : null;
  const htmlSource = siblingHtml && fs.existsSync(siblingHtml) ? fs.readFileSync(siblingHtml, 'utf8') : '';
  if (!htmlSource.includes('| translate')) {
    continue;
  }
  ensureTranslatePipe(componentPath);
}

fs.writeFileSync(frPath, `${JSON.stringify(unflattenObject(frFlat), null, 2)}\n`, 'utf8');
fs.writeFileSync(enPath, `${JSON.stringify(unflattenObject(enFlat), null, 2)}\n`, 'utf8');

function ensureTranslation(fileNamespace, rawText, textCounts) {
  const normalizedFr = normalizeWhitespace(rawText);
  const existingKey = existingFrToKey.get(normalizedFr);
  if (existingKey) {
    return existingKey;
  }

  const baseKey = `${fileNamespace}.${toKeySegment(normalizedFr) || 'TEXT'}`;
  const count = textCounts.get(baseKey) ?? 0;
  textCounts.set(baseKey, count + 1);
  const finalKey = count === 0 ? baseKey : `${baseKey}_${count + 1}`;

  frFlat[finalKey] = normalizedFr;
  enFlat[finalKey] = translateToEnglish(normalizedFr);
  existingFrToKey.set(normalizedFr, finalKey);
  return finalKey;
}

function translateToEnglish(frText) {
  const existing = Object.entries(frFlat).find(([, value]) => value === frText)?.[0];
  if (existing && enFlat[existing]) {
    return enFlat[existing];
  }
  if (legacyTranslations[frText]) {
    return legacyTranslations[frText];
  }
  return frText;
}

function shouldSkipAttribute(attr, value) {
  const trimmed = value.trim();
  return (
    !trimmed ||
    trimmed.includes('{{') ||
    trimmed.includes('| translate') ||
    trimmed.startsWith('http') ||
    trimmed.startsWith('/') ||
    trimmed.startsWith('data:') ||
    trimmed.startsWith('assets/') ||
    trimmed.startsWith('images/') ||
    trimmed.includes('.') ||
    !/[A-Za-zÀ-ÿ]/.test(trimmed)
  );
}

function shouldSkipText(text) {
  const normalized = normalizeWhitespace(text);
  return (
    !normalized ||
    normalized.includes('{{') ||
    normalized.includes('}}') ||
    normalized.includes('| translate') ||
    normalized.startsWith('@if') ||
    normalized.startsWith('@for') ||
    normalized.startsWith('@switch') ||
    normalized.startsWith('<!--') ||
    !/[A-Za-zÀ-ÿ]/.test(normalized)
  );
}

function normalizeWhitespace(value) {
  return value.replace(/\s+/g, ' ').trim();
}

function buildFileNamespace(filePath) {
  const relative = path.relative(appDir, filePath).replace(/\\/g, '/').replace(/\.component\.html$/, '').replace(/\.html$/, '');
  return `AUTO.${relative.split('/').map(toKeySegment).filter(Boolean).join('.')}`;
}

function toKeySegment(value) {
  return stripAccents(value)
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '')
    .slice(0, 60);
}

function stripAccents(value) {
  return value.normalize('NFD').replace(/\p{M}/gu, '');
}

function walk(directory, predicate) {
  const results = [];
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    const fullPath = path.join(directory, entry.name);
    if (entry.isDirectory()) {
      results.push(...walk(fullPath, predicate));
      continue;
    }
    if (predicate(fullPath)) {
      results.push(fullPath);
    }
  }
  return results;
}

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, 'utf8'));
}

function flattenObject(value, prefix = '', output = {}) {
  for (const [key, nestedValue] of Object.entries(value)) {
    const nextPrefix = prefix ? `${prefix}.${key}` : key;
    if (nestedValue && typeof nestedValue === 'object' && !Array.isArray(nestedValue)) {
      flattenObject(nestedValue, nextPrefix, output);
      continue;
    }
    output[nextPrefix] = nestedValue;
  }
  return output;
}

function unflattenObject(flat) {
  const result = {};
  for (const [key, value] of Object.entries(flat)) {
    const segments = key.split('.');
    let cursor = result;
    for (let index = 0; index < segments.length - 1; index += 1) {
      const segment = segments[index];
      cursor[segment] ??= {};
      cursor = cursor[segment];
    }
    cursor[segments.at(-1)] = value;
  }
  return sortObject(result);
}

function sortObject(value) {
  if (Array.isArray(value) || value === null || typeof value !== 'object') {
    return value;
  }

  const sorted = {};
  for (const key of Object.keys(value).sort()) {
    sorted[key] = sortObject(value[key]);
  }
  return sorted;
}

function loadLegacyTranslations(filePath) {
  const source = fs.readFileSync(filePath, 'utf8');
  const match = source.match(/const LEGACY_EN_TRANSLATIONS: Record<string, string> = \{([\s\S]*?)\n\};/);
  if (!match) {
    return {};
  }

  const body = match[1];
  const translations = {};
  const regex = /'((?:\\'|[^'])+)':\s*'((?:\\'|[^'])+)'/g;
  let current = regex.exec(body);
  while (current) {
    translations[current[1].replace(/\\'/g, "'")] = current[2].replace(/\\'/g, "'");
    current = regex.exec(body);
  }
  return translations;
}

function ensureTranslatePipe(componentPath) {
  let source = fs.readFileSync(componentPath, 'utf8');
  if (!source.includes('templateUrl:') || !source.includes('imports: [')) {
    return;
  }

  if (!source.includes("from '@ngx-translate/core'")) {
    source = `import { TranslatePipe } from '@ngx-translate/core';\n${source}`;
  } else if (!source.includes('TranslatePipe')) {
    source = source.replace(
      /import\s*\{([^}]+)\}\s*from '@ngx-translate\/core';/,
      (fullMatch, imports) => `import {${imports.trim()}, TranslatePipe} from '@ngx-translate/core';`,
    );
  }

  source = source.replace(/imports:\s*\[([\s\S]*?)\]/m, (fullMatch, importsBlock) => {
    if (importsBlock.includes('TranslatePipe')) {
      return fullMatch;
    }

    const trimmedBlock = importsBlock.trim();
    if (!trimmedBlock) {
      return 'imports: [TranslatePipe]';
    }

    return `imports: [${trimmedBlock}, TranslatePipe]`;
  });

  fs.writeFileSync(componentPath, source, 'utf8');
}
