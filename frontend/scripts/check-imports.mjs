// 校验所有 .vue/.js 的相对 import 路径目标是否存在
import { readFileSync, readdirSync, statSync, existsSync } from 'node:fs'
import { join, resolve, dirname } from 'node:path'

const root = resolve(process.argv[2] || '.')
const files = []
;(function walk(dir) {
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry)
    if (entry === 'node_modules' || entry === 'dist' || entry === '.git' || entry === '.npm-cache') continue
    if (statSync(full).isDirectory()) walk(full)
    else if (full.endsWith('.vue') || full.endsWith('.js') || full.endsWith('.mjs')) files.push(full)
  }
})(root)

let failed = 0
let checked = 0
for (const file of files) {
  const src = readFileSync(file, 'utf-8')
  const importRe = /(?:import\s+[^'"]*?from\s*|import\s*\(|from\s*)['"]([^'"]+)['"]/g
  let m
  while ((m = importRe.exec(src)) !== null) {
    const spec = m[1]
    if (!spec.startsWith('.')) continue // 仅相对路径
    checked++
    const base = dirname(file)
    const target = resolve(base, spec)
    // 解析可能缺扩展名的路径
    let ok = existsSync(target)
    if (!ok && !/\.[a-z]+$/i.test(spec)) {
      ok = ['.vue', '.js', '.mjs', '.css', '.json'].some((ext) => existsSync(target + ext))
    }
    if (!ok) {
      failed++
      console.log(`MISSING ${file}: import '${spec}' -> ${target}`)
    }
  }
}
console.log(`\nChecked ${checked} relative imports in ${files.length} files, ${failed} missing.`)
process.exit(failed ? 1 : 0)
