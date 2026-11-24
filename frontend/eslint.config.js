import js from '@eslint/js';
import { defineConfig, globalIgnores } from 'eslint/config';
import prettier from 'eslint-config-prettier/flat';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import globals from 'globals';
import tseslint from 'typescript-eslint';

const FEATURES = ['auth', 'books', 'cart', 'orders'];

/** app → features → shared, never feature → feature. Any feature may use auth's public API ('@/features/auth'). */
function featureBoundaries(feature) {
  const otherFeatures = FEATURES.filter((other) => other !== feature && other !== 'auth');
  const patterns = [
    ...otherFeatures.map((other) => ({
      group: [`@/features/${other}`, `@/features/${other}/**`],
      message: `Features must not import each other (${feature} → ${other}). Combine them in app/ (e.g. the router).`,
    })),
    {
      group: ['@/app', '@/app/**'],
      message: 'Features must not import from app/: dependencies flow app → features → shared.',
    },
    { group: ['@/testing', '@/testing/**'], message: 'Production code must not import test helpers.' },
  ];
  if (feature !== 'auth') {
    patterns.push({
      group: ['@/features/auth/**'],
      message: "Use auth's public API: import from '@/features/auth', not its internal files.",
    });
  }
  return {
    files: [`src/features/${feature}/**/*.{ts,tsx}`],
    ignores: ['**/*.test.{ts,tsx}', '**/__tests__/**'],
    rules: { 'no-restricted-imports': ['error', { patterns }] },
  };
}

export default defineConfig([
  globalIgnores(['dist', 'coverage']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      tseslint.configs.recommended,
      reactHooks.configs.flat['recommended-latest'],
      reactRefresh.configs.vite,
    ],
    languageOptions: { ecmaVersion: 2022, globals: globals.browser },
  },

  ...FEATURES.map(featureBoundaries),
  {
    files: ['src/app/**/*.{ts,tsx}'],
    ignores: ['**/*.test.{ts,tsx}', '**/__tests__/**'],
    rules: {
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            {
              group: ['@/features/*/**'],
              message: "app/ uses a feature's public API (its index.ts): import from '@/features/<name>'.",
            },
            { group: ['@/testing', '@/testing/**'], message: 'Production code must not import test helpers.' },
          ],
        },
      ],
    },
  },
  {
    files: ['src/{components,hooks,lib,types,utils}/**/*.{ts,tsx}'],
    ignores: ['**/*.test.{ts,tsx}'],
    rules: {
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            {
              group: ['@/features', '@/features/**', '@/app', '@/app/**'],
              message: 'Shared code must not depend on app/ or features/: they depend on it.',
            },
            { group: ['@/testing', '@/testing/**'], message: 'Production code must not import test helpers.' },
          ],
        },
      ],
    },
  },

  // tests may reach into any file, so the boundaries do not apply to them
  {
    files: ['**/*.test.{ts,tsx}', '**/__tests__/**', 'src/testing/**'],
    languageOptions: { globals: { ...globals.browser, ...globals.node, ...globals.vitest } },
    rules: { 'react-refresh/only-export-components': 'off' },
  },

  // last: turns off ESLint rules that would conflict with Prettier's formatting
  prettier,
]);
