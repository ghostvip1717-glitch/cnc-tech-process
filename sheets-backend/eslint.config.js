import js from '@eslint/js';
import globals from 'globals';

/** Apps Script .gs + pure JS (V8 / ES2019). */
export default [
  {
    ignores: ['ONE_FILE.gs', 'node_modules/**', 'tech_process/TechProcessRules.gs'],
  },
  js.configs.recommended,
  {
    files: ['**/*.gs'],
    languageOptions: {
      ecmaVersion: 2019,
      sourceType: 'script',
      globals: {
        ...globals.es2019,
        console: 'readonly',
      },
    },
    rules: {
      // Cross-file globals are normal in Apps Script; unused locally ≠ unused.
      'no-unused-vars': 'off',
      'no-undef': 'off',
    },
  },
  {
    files: ['pure/**/*.js'],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'module',
      globals: {
        ...globals.es2022,
      },
    },
    rules: {
      'no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
    },
  },
  {
    files: ['scripts/**/*.mjs', 'vitest.config.js', 'eslint.config.js', 'tests/**/*.js'],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'module',
      globals: {
        ...globals.node,
        ...globals.es2022,
      },
    },
  },
];
