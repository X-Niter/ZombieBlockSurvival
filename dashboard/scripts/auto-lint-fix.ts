import fs from 'fs';
import path from 'path';

function walk(dir: string, filelist: string[] = []) {
  const files = fs.readdirSync(dir);
  for (const file of files) {
    const filepath = path.join(dir, file);
    if (fs.statSync(filepath).isDirectory()) {
      walk(filepath, filelist);
    } else if (file.endsWith('.ts') || file.endsWith('.tsx')) {
      filelist.push(filepath);
    }
  }
  return filelist;
}

function fixSyntaxIssues(file: string) {
  let content = fs.readFileSync(file, 'utf-8');
  const original = content;

  content = content
    .replace(/" \+ \(process\.env\./g, '${process.env.')
    .replace(/"\+\(process\.env\./g, '${process.env.')
    .replace(/Authorization: "Bearer \\?\(process\.env\.GITHUB_TOKEN \|\| ""\)\`? }/g,
      'Authorization: `Bearer ${process.env.GITHUB_TOKEN || ""}` }')
    .replace(/Authorization: "Bearer \$\{process\.env\./g, 'Authorization: `Bearer ${process.env.')
    .replace(/}\` }/g, '` }')
    .replace(/}` }/g, '` }');

  if (content !== original) {
    fs.writeFileSync(file, content);
    console.log(`Fixed: ${file}`);
  }
}

const base = path.resolve(__dirname, '../src');
const tsFiles = walk(base);

tsFiles.forEach(fixSyntaxIssues);
