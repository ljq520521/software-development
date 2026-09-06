// 静态验证所有 .vue 文件:SFC parse + script 编译 + template 编译
// 不启动 esbuild/子进程,仅用 @vue/compiler-sfc 做语法级校验
import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join, resolve } from 'node:path'
import { parse, compileScript, compileTemplate } from '@vue/compiler-sfc'

const root = resolve(process.argv[2] || '.')
const vueFiles = []
;(function walk(dir) {
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry)
    if (entry === 'node_modules' || entry === 'dist' || entry === '.git') continue
    if (statSync(full).isDirectory()) walk(full)
    else if (full.endsWith('.vue')) vueFiles.push(full)
  }
})(root)

let failed = 0
for (const file of vueFiles) {
  const source = readFileSync(file, 'utf-8')
  const errors = []
  let scriptErrors = []
  let templateErrors = []
  try {
    const { descriptor, errors: parseErrors } = parse(source, { filename: file })
    errors.push(...parseErrors.map((e) => `[parse] ${e.message}`))
    if (descriptor.script || descriptor.scriptSetup) {
      try {
        const s = compileScript(descriptor, { id: 'static-check' })
        scriptErrors = s.errors || []
      } catch (e) {
        scriptErrors = [e.message]
      }
    }
    if (descriptor.template && !descriptor.template.errors?.length) {
      try {
        const t = compileTemplate({
          source: descriptor.template.content,
          filename: file,
          id: 'static-check',
        })
        templateErrors = t.errors || []
      } catch (e) {
        templateErrors = [e.message]
      }
    }
  } catch (e) {
    errors.push(`[fatal] ${e.message}`)
  }
  const all = [...errors, ...scriptErrors.map((m) => `[script] ${m}`), ...templateErrors.map((m) => `[template] ${m}`)]
  if (all.length) {
    failed++
    console.log(`FAIL ${file}`)
    for (const m of all) console.log(`     ${m}`)
  } else {
    console.log(`OK   ${file}`)
  }
}
console.log(`\nChecked ${vueFiles.length} files, ${failed} failed.`)
process.exit(failed ? 1 : 0)
