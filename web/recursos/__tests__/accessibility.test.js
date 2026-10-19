/**
 * Suite de pruebas de accesibilidad (a11y) de GameBoXD.
 *
 * Carga las páginas HTML reales en jsdom y comprueba:
 *  - Todo botón y enlace solo-icono tiene nombre accesible (texto visible o
 *    aria-label / aria-labelledby).
 *  - style.css restaura un anillo de foco visible mediante :focus-visible
 *    (sin dejar outline: none sin sustituto).
 *  - Los contenedores de toast usan aria-live="polite".
 *  - El texto informativo de búsqueda usa aria-live="polite".
 *  - Toda imagen tiene atributo alt (vacío si es puramente decorativa).
 *  - Los toasts creados por showToast() (recursos/ui.js) llevan un botón de
 *    cierre con aria-label.
 */

import { describe, it, expect, beforeAll, afterEach } from 'vitest'
import { showToast } from '../../recursos/ui.js'
import { JSDOM, VirtualConsole } from 'jsdom'
import fs from 'fs'
import path from 'path'

const WEB_ROOT = path.resolve(__dirname, '..', '..')
const PAGES = [
  'index.html',
  'busqueda.html',
  'detalleJuego.html',
  'insertar.html',
  'login.html',
  'miLista.html',
]

let documents = {}
let css = ''

beforeAll(() => {
  css = fs.readFileSync(path.join(WEB_ROOT, 'recursos/style.css'), 'utf8')

  for (const page of PAGES) {
    const html = fs.readFileSync(path.join(WEB_ROOT, page), 'utf8')
    const dom = new JSDOM(html, {
      url: `http://localhost/${page}`,
      // Sin red: no cargar subrecursos externos de las páginas
      virtualConsole: new VirtualConsole(),
    })
    documents[page] = dom.window.document
  }
})

afterEach(() => {
  // Limpiar toasts creados por showToast en el documento global de vitest
  const container = document.getElementById('toast-container')
  if (container) container.remove()
})

/**
 * Calcula el nombre accesible aproximado de un botón/enlace.
 * Un "×" aislado es un icono visual (tacha de cierre), no un nombre accesible.
 */
function accessibleName(el) {
  const label = el.getAttribute('aria-label')
  if (label) return label
  if (el.getAttribute('aria-labelledby')) return true

  const text = el.textContent.trim()
  if (text && text !== '×') return text

  return ''
}

describe('Accesibilidad: botones y enlaces solo-icono', () => {
  it('todo botón y enlace solo-icono tiene nombre accesible (texto o aria-label/aria-labelledby)', () => {
    for (const [page, doc] of Object.entries(documents)) {
      const focusableButtons = [
        ...doc.querySelectorAll('button'),
        ...doc.querySelectorAll('a.icon-button'),
      ]

      for (const el of focusableButtons) {
        const name = accessibleName(el)
        const html = el.outerHTML.replace(/\s+/g, ' ').slice(0, 100)
        expect(
          name,
          `${page}: el botón/enlace sin nombre accesible: ${html}`,
        ).toBeTruthy()
      }
    }
  })
})

describe('Accesibilidad: indicador de foco visible (style.css)', () => {
  it('style.css define una regla :focus-visible con contorno visible', () => {
    const focusVisibleBlock = css.match(/:focus-visible[^{]*\{[\s\S]*?\}/)
    expect(focusVisibleBlock).toBeTruthy(
      'style.css debería contener una regla :focus-visible',
    )
    expect(focusVisibleBlock[0]).toMatch(
      /outline:\s*(?!none\b)/i,
      'la regla :focus-visible debería definir un contorno visible (no outline: none)',
    )
  })

  it('style.css mantiene outline: none solo para el foco del ratón (:focus:not(:focus-visible))', () => {
    const focusBlock = css.match(
      /button:focus:not\(:focus-visible\)[^{]*\{[\s\S]*?\}/,
    )
    expect(focusBlock).toBeTruthy(
      'style.css debería conservar outline: none en :focus:not(:focus-visible) para UX de ratón',
    )
    expect(focusBlock[0]).toMatch(/outline:\s*none\b/i)
  })

  it('el bloque de reset no deshabilita el contorno de foco indiscriminadamente', () => {
    const resetBlock = css.match(
      /button\s*,\s*input\s*,\s*select\s*,\s*textarea\s*\{[\s\S]*?\}/,
    )
    expect(resetBlock).toBeTruthy()
    expect(resetBlock[0]).not.toMatch(/outline:/i)
  })
})

describe('Accesibilidad: regiones en vivo (aria-live)', () => {
  it('cada #toast-container tiene aria-live="polite"', () => {
    let containersFound = 0
    for (const doc of Object.values(documents)) {
      const container = doc.getElementById('toast-container')
      if (container) {
        containersFound++
        expect(container.getAttribute('aria-live')).toBe('polite')
      }
    }
    expect(containersFound).toBeGreaterThan(0)
  })

  it('el texto informativo de resultados de búsqueda tiene aria-live="polite"', () => {
    const el = documents['busqueda.html'].getElementById('results-info-text')
    expect(el).toBeTruthy('#results-info-text debería existir en busqueda.html')
    expect(el.getAttribute('aria-live')).toBe('polite')
  })
})

describe('Accesibilidad: imágenes con alt', () => {
  it('cada  tiene un atributo alt (vacío si es decorativa)', () => {
    for (const [page, doc] of Object.entries(documents)) {
      const imgs = doc.querySelectorAll('img')
      for (const img of imgs) {
        expect(
          img.hasAttribute('alt'),
          `${page}: la imagen ${img.getAttribute('src')} no tiene atributo alt`,
        ).toBe(true)
      }
    }
  })
})

describe('Accesibilidad: toasts generados dinámicamente (showToast)', () => {
  it('el botón de cierre de un toast creado por showToast() tiene aria-label', () => {
    const container = document.createElement('div')
    container.id = 'toast-container'
    document.body.appendChild(container)

    showToast('Mensaje de prueba de accesibilidad', 'error')

    const closeBtn = container.querySelector('.toast-close')
    expect(closeBtn).toBeTruthy(
      'showToast() debería crear un toast con botón de cierre',
    )
    expect(closeBtn.getAttribute('aria-label')).toBe(
      'Cerrar notificación',
      'el botón de cierre del toast debería tener aria-label',
    )
  })
})
