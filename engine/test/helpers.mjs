import { readFileSync, readdirSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

export const rulesDir = join(dirname(fileURLToPath(import.meta.url)), '..', '..', 'rules');

export function loadRuleSet(platform) {
  return JSON.parse(readFileSync(join(rulesDir, `${platform}.json`), 'utf8'));
}

export function allRuleSets() {
  return readdirSync(rulesDir)
    .filter((f) => f.endsWith('.json'))
    .map((f) => ({ file: f, ruleSet: JSON.parse(readFileSync(join(rulesDir, f), 'utf8')) }));
}
