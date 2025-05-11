module.exports = {
  root: true,
  parser: '@typescript-eslint/parser',
  plugins: ['@typescript-eslint'],
  extends: ['plugin:@typescript-eslint/recommended'],
  rules: {
    '@typescript-eslint/no-unused-vars': 'warn',
    'no-useless-escape': 'error',
    'quotes': ['error', 'backtick'],
  },
};
