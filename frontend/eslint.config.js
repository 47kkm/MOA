import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tseslint from 'typescript-eslint'
import { defineConfig, globalIgnores } from 'eslint/config'

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      tseslint.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      globals: globals.browser,
    },
  },
  // 0.2 모듈 경계 ①: 다른 모듈은 index.ts로만 (폴더 이름까지만)
  {
    files: ['src/features/**/*.{ts,tsx}'],
    rules: {
      'no-restricted-imports': ['error', {
        patterns: [{
          group: ['../*/*', '!../../**'],
          message: '다른 모듈은 index.ts로만 가져오세요 (04 프론트 0.2)',
        }],
      }],
    },
  },
  // 0.2 모듈 경계 ②: shared는 features를 모른다
  {
    files: ['src/shared/**/*.{ts,tsx}'],
    rules: {
      'no-restricted-imports': ['error', {
        patterns: [{
          group: ['**/features/**'],
          message: 'shared는 features를 가져오지 않아요 (04 프론트 0.2)',
        }],
      }],
    },
  },
])
