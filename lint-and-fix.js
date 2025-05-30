// Lint and fix script using ESLint
const { ESLint } = require("eslint");

(async function main() {
  const eslint = new ESLint({ fix: true });
  const results = await eslint.lintFiles(["dashboard/**/*.ts", "dashboard/**/*.tsx"]);
  await ESLint.outputFixes(results);
  const formatter = await eslint.loadFormatter("stylish");
  const resultText = formatter.format(results);
  console.log(resultText);
})();
