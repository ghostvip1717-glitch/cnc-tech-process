#!/usr/bin/env node
/**
 * Sync pure/techProcessRules.js → tech_process/TechProcessRules.gs (GAS globals).
 */
import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
let src = readFileSync(join(root, 'pure/techProcessRules.js'), 'utf8');

src = src
  .replace(/^\/\*\*[\s\S]*?\*\/\n+/m, '')
  .replace(/^export const /gm, 'var ')
  .replace(/^export function /gm, 'function ');

const out = `/**
 * AUTO-GENERATED from pure/techProcessRules.js — do not edit by hand.
 * Run: npm run sync:rules
 */

${src.trim()}
`;
writeFileSync(join(root, 'tech_process/TechProcessRules.gs'), out);
console.log('synced tech_process/TechProcessRules.gs');
