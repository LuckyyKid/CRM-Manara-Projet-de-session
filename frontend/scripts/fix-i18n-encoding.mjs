import fs from 'node:fs';
import path from 'node:path';

const rootDir = process.cwd();
const files = [
  path.join(rootDir, 'src', 'assets', 'i18n', 'fr.json'),
  path.join(rootDir, 'src', 'assets', 'i18n', 'en.json'),
];

for (const filePath of files) {
  const content = JSON.parse(fs.readFileSync(filePath, 'utf8'));
  const fixed = fixNode(content);
  fs.writeFileSync(filePath, `${JSON.stringify(fixed, null, 2)}\n`, 'utf8');
}

function fixNode(node) {
  if (Array.isArray(node)) {
    return node.map(fixNode);
  }

  if (node && typeof node === 'object') {
    return Object.fromEntries(Object.entries(node).map(([key, value]) => [key, fixNode(value)]));
  }

  if (typeof node === 'string') {
    return fixMojibake(node);
  }

  return node;
}

function fixMojibake(value) {
  const manuallyFixed = value
    .replace(/Ã‰/g, 'É')
    .replace(/Ã€/g, 'À')
    .replace(/Ã‚/g, 'Â')
    .replace(/Ã‡/g, 'Ç')
    .replace(/Ãˆ/g, 'È')
    .replace(/Ã‰/g, 'É')
    .replace(/ÃŠ/g, 'Ê')
    .replace(/Ã‹/g, 'Ë')
    .replace(/ÃŽ/g, 'Î')
    .replace(/ÃÏ/g, 'Ï')
    .replace(/Ã”/g, 'Ô')
    .replace(/Ã™/g, 'Ù')
    .replace(/Ã›/g, 'Û')
    .replace(/Ãœ/g, 'Ü')
    .replace(/Ã /g, 'à')
    .replace(/Ã¡/g, 'á')
    .replace(/Ã¢/g, 'â')
    .replace(/Ã£/g, 'ã')
    .replace(/Ã¤/g, 'ä')
    .replace(/Ã§/g, 'ç')
    .replace(/Ã¨/g, 'è')
    .replace(/Ã©/g, 'é')
    .replace(/Ãª/g, 'ê')
    .replace(/Ã«/g, 'ë')
    .replace(/Ã¬/g, 'ì')
    .replace(/Ã­/g, 'í')
    .replace(/Ã®/g, 'î')
    .replace(/Ã¯/g, 'ï')
    .replace(/Ã²/g, 'ò')
    .replace(/Ã³/g, 'ó')
    .replace(/Ã´/g, 'ô')
    .replace(/Ã¶/g, 'ö')
    .replace(/Ã¹/g, 'ù')
    .replace(/Ãº/g, 'ú')
    .replace(/Ã»/g, 'û')
    .replace(/Ã¼/g, 'ü')
    .replace(/Å“/g, 'œ')
    .replace(/Å’/g, 'Œ')
    .replace(/Â«/g, '«')
    .replace(/Â»/g, '»')
    .replace(/Â°/g, '°')
    .replace(/Â :/g, ':')
    .replace(/Â ?/g, '?')
    .replace(/Â !/g, '!')
    .replace(/Â;/g, ';')
    .replace(/Â,/g, ',')
    .replace(/Â/g, '');

  if (!/[ÃÂÅ]/.test(manuallyFixed)) {
    return manuallyFixed;
  }

  const transcoded = Buffer.from(manuallyFixed, 'latin1').toString('utf8');
  return scoreString(transcoded) >= scoreString(manuallyFixed) ? transcoded : manuallyFixed;
}

function scoreString(value) {
  const suspiciousChars = (value.match(/[ÃÂÅ�]/g) ?? []).length;
  const accentedChars = (value.match(/[À-ÿœŒ]/g) ?? []).length;
  return accentedChars - suspiciousChars;
}
