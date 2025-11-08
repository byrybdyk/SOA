<template>
  <div class="page">
    <header class="topbar">
      <h2>Фильмы</h2>
      <div class="right">
        <label class="size">
          На странице:
          <select v-model.number="size" @change="onSizeChange">
            <option :value="10">10</option>
            <option :value="20">20</option>
            <option :value="50">50</option>
          </select>
        </label>
      </div>
    </header>

    <div class="card transfer-card">
      <div class="transfer-row">
        <div class="transfer-title">Перераспределить «Оскары» между жанрами</div>
        <div class="transfer-controls">
          <label class="inline">
            Из жанра:
            <select class="input select" v-model="fromGenre">
              <option disabled value="">Выберите жанр</option>
              <option v-for="g in GENRE_OPTIONS" :key="g.value" :value="g.value">
                {{ g.label }}
              </option>
            </select>
          </label>
          <label class="inline">
            В жанр:
            <select class="input select" v-model="toGenre">
              <option disabled value="">Выберите жанр</option>
              <option v-for="g in GENRE_OPTIONS" :key="g.value" :value="g.value">
                {{ g.label }}
              </option>
            </select>
          </label>

          <button class="btn" :disabled="!canTransfer || transferLoading" @click="onTransfer">
            {{ transferLoading ? 'Выполняю…' : 'Перераспределить' }}
          </button>
        </div>
      </div>

      <p v-if="transferResult" class="success">
        Перераспределено «Оскаров»: <b>{{ transferResult }}</b>
      </p>
      <p v-if="transferError" class="error">{{ transferError }}</p>
    </div>

    <div class="filters card">
      <div class="filters-header">
        <strong>Фильтры</strong>
        <div class="filters-actions">
          <button class="link" @click="addFilter">+ условие</button>
          <button class="link danger" @click="clearFilters" :disabled="filters.length === 0">
            Очистить
          </button>
          <button class="btn" @click="applyFilters" :disabled="!areFiltersValid">Применить</button>
        </div>
      </div>

      <div class="filter-rows">
        <div class="filter-row" v-for="(f, idx) in filters" :key="f.id">
          <select class="input select" v-model="f.field" @change="onFieldChange(f)">
            <option v-for="fld in FIELDS" :key="fld.key" :value="fld.key">{{ fld.label }}</option>
          </select>

          <select class="input select" v-model="f.op">
            <option v-for="op in opsFor(f.field)" :key="op" :value="op">{{ OP_LABELS[op] }}</option>
          </select>

          <template v-if="inputKind(f) === 'enum-multi' && f.op === 'in'">
            <select class="input select" v-model="f.values" multiple>
              <option v-for="opt in enumOptionsFor(f.field)" :key="opt" :value="opt">
                {{ f.field === 'genre' ? genreLabel(opt) : opt }}
              </option>
            </select>
          </template>

          <template v-else-if="inputKind(f) === 'enum'">
            <select class="input select" v-model="f.value">
              <option value="" disabled>Выберите…</option>
              <option v-for="opt in enumOptionsFor(f.field)" :key="opt" :value="opt">
                {{ f.field === 'genre' ? genreLabel(opt) : opt }}
              </option>
            </select>
          </template>
          <template v-else-if="isNumberField(f.field)">
            <template v-if="f.op === 'in'">
              <input
                class="input"
                type="text"
                :value="typeof f.value === 'string' ? f.value : ''"
                :placeholder="
                  'Через запятую: ' + (isFloatField(f.field) ? '1.23, 4.56' : '1, 2, 3')
                "
                @input="onFilterNumberCsvInput($event, f)"
                :class="{ invalid: !!f._invalid }"
              />
            </template>
            <template v-else>
              <input
                class="input"
                type="text"
                :value="f.value"
                :inputmode="isFloatField(f.field) ? 'decimal' : 'numeric'"
                @input="onFilterNumberInput($event, f)"
                :placeholder="'Значение'"
                :class="{ invalid: !!f._invalid }"
              />
            </template>
          </template>

          <template v-else-if="inputKind(f) === 'date'">
            <input class="input" type="date" v-model="f.value" />
          </template>

          <template v-else>
            <input
              class="input"
              :type="isNumberField(f.field) ? 'text' : 'text'"
              v-model="f.value"
              :placeholder="f.op === 'in' ? 'Через запятую: a,b,c' : 'Значение'"
            />
          </template>

          <button class="link danger" @click="removeFilter(idx)">Удалить</button>
        </div>
        <p class="hint">Поддерживаются операторы: =, ≠, &gt;, ≥, &lt;, ≤</p>
      </div>
    </div>
    <div class="card" style="margin-bottom: 12px; padding: 12px">
      <button class="btn" @click="openCreate">+ Добавить фильм</button>
    </div>

    <div class="card">
      <table class="table">
        <thead>
          <tr>
            <th
              v-for="col in COLUMNS"
              :key="col.key"
              @click="toggleSort(col.key)"
              :class="thClass(col.key)"
            >
              {{ col.title }}
            </th>
            <th class="actions-col">Действия</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="m in rows" :key="m.id">
            <td v-for="col in COLUMNS" :key="col.key">
              {{ formatValue(getVal(m, col.key), col.key) }}
            </td>

            <td class="actions">
              <button
                class="edit-btn"
                :disabled="!!editing"
                @click="openEdit(m)"
                title="Редактировать"
              >
                ✏️
              </button>
              <button
                class="trash-btn"
                :disabled="deletingId === m.id"
                @click="onDelete(m)"
                title="Удалить фильм"
              >
                🗑
              </button>
            </td>
          </tr>
          <tr v-if="!loading && rows.length === 0">
            <td :colspan="COLUMNS.length" class="empty">Ничего не найдено</td>
          </tr>
        </tbody>
      </table>

      <div v-if="loading" class="loading">Загрузка…</div>
      <div v-if="error" class="error">{{ error }}</div>
    </div>

    <div class="pager" v-if="totalPages > 1">
      <button class="pg-btn" :disabled="page === 0" @click="goTo(page - 1)">‹</button>
      <span class="pg-info">Стр. {{ page + 1 }} из {{ totalPages }}</span>
      <button class="pg-btn" :disabled="page >= totalPages - 1" @click="goTo(page + 1)">›</button>
    </div>

    <div v-if="editing" class="modal-backdrop" @click.self="cancelEdit">
      <div class="modal">
        <h3 v-if="!isCreateMode">Редактирование фильма #{{ editId }}</h3>
        <h3 v-else>Создание фильма</h3>

        <div class="form">
          <label>
            Название
            <input
              v-model="editForm.name"
              type="text"
              :class="{
                invalid: editForm.name == null || !editForm.name.trim(),
              }"
            />
          </label>

          <div class="two">
            <label>
              Координата X
              <input
                type="text"
                inputmode="numeric"
                :value="editForm.coordinates.x"
                @input="(e) => onIntegerInput(e, 'coordinates.x')"
                :class="{
                  invalid:
                    !Number.isInteger(editForm.coordinates.x) ||
                    normalizeNumber(editForm.coordinates.x) === false ||
                    !inRangeIntMinusOne(editForm.coordinates.x),
                }"
              />
            </label>
            <label>
              Координата Y
              <input
                class="input"
                type="text"
                inputmode="decimal"
                :value="editForm.coordinates.y"
                @input="onCoordYInput"
                :class="{
                  invalid:
                    normalizeNumber(editForm.coordinates.y) === false ||
                    !inRangeFloatMinusOne(editForm.coordinates.y),
                }"
              />
            </label>
          </div>

          <div class="two">
            <label>
              Оскаров
              <input
                type="text"
                inputmode="numeric"
                :value="editForm.oscarsCount"
                @input="(e) => onIntegerInput(e, 'oscarsCount')"
                :class="{
                  invalid:
                    !Number.isInteger(editForm.oscarsCount) ||
                    editForm.oscarsCount < 0 ||
                    !inRangeNonNegativeIntMinusOne(editForm.oscarsCount),
                }"
              />
            </label>
            <label>
              Жанр
              <select v-model="editForm.genre">
                <option v-for="g in GENRE_OPTIONS" :key="g.value" :value="g.value">
                  {{ g.label }}
                </option>
              </select>
            </label>
          </div>

          <label>
            MPAA рейтинг
            <select v-model="editForm.mpaaRating">
              <option v-for="m in MPAA" :key="m" :value="m">{{ m }}</option>
            </select>
          </label>

          <fieldset class="fieldset">
            <legend>Оператор</legend>
            <label>
              Имя
              <input
                v-model="editForm.operator.name"
                type="text"
                :class="{
                  invalid: editForm.operator.name == null || !editForm.operator.name.trim(),
                }"
              />
            </label>
            <div class="two">
              <label>
                Рост
                <input
                  class="input"
                  type="text"
                  inputmode="decimal"
                  :value="editForm.operator.height"
                  @input="onHeightInput"
                  :class="{
                    invalid:
                      normalizeNumber(editForm.operator.height) === false ||
                      !inRangeFloatMinusOne(editForm.operator.height),
                  }"
                />
              </label>
              <label>
                Вес
                <input
                  type="text"
                  inputmode="numeric"
                  :value="editForm.operator.weight"
                  @input="(e) => onIntegerInput(e, 'operator.weight')"
                  class="input"
                  :class="{
                    invalid:
                      !Number.isInteger(editForm.operator.weight) ||
                      editForm.operator.weight <= 0 ||
                      !inRangeNonNegativeIntMinusOne(editForm.operator.weight),
                  }"
                />
              </label>
            </div>
            <div class="three">
              <label>
                Локация X
                <input
                  type="text"
                  inputmode="numeric"
                  :value="editForm.operator.location.x"
                  @input="(e) => onIntegerInput(e, 'operator.location.x')"
                  class="input"
                  :class="{
                    invalid:
                      normalizeNumber(editForm.operator.location.x) === false ||
                      !Number.isInteger(editForm.operator.location.x) ||
                      !inRangeIntMinusOne(editForm.operator.location.x),
                  }"
                />
              </label>

              <label>
                Локация Y
                <input
                  type="text"
                  inputmode="numeric"
                  :value="editForm.operator.location.y"
                  @input="(e) => onIntegerInput(e, 'operator.location.y')"
                  class="input"
                  :class="{
                    invalid:
                      normalizeNumber(editForm.operator.location.y) === false ||
                      !Number.isInteger(editForm.operator.location.y) ||
                      !inRangeIntMinusOne(editForm.operator.location.y),
                  }"
                />
              </label>

              <label>
                Локация Z
                <input
                  type="text"
                  inputmode="numeric"
                  :value="editForm.operator.location.z"
                  @input="(e) => onIntegerInput(e, 'operator.location.z')"
                  class="input"
                  :class="{
                    invalid:
                      normalizeNumber(editForm.operator.location.z) === false ||
                      !Number.isInteger(editForm.operator.location.z) ||
                      !inRangeIntMinusOne(editForm.operator.location.z),
                  }"
                />
              </label>
            </div>
          </fieldset>
        </div>

        <div class="modal-actions">
          <button class="btn" :disabled="saving || !isFormValid" @click="saveEdit">
            {{
              saving
                ? isCreateMode
                  ? 'Создаю…'
                  : 'Сохраняю…'
                : isCreateMode
                  ? 'Создать'
                  : 'Сохранить'
            }}
          </button>
          <button class="link" :disabled="saving" @click="cancelEdit">Отмена</button>
        </div>

        <p class="error" v-if="editError">{{ editError }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { searchMovies, updateMovie, deleteMovie, createMovie } from '../api/movies'
import { redistributeRewards } from '../api/genres'

const COLUMNS = [
  { title: 'ID', key: 'id' },
  { title: 'Название', key: 'name' },
  { title: 'Дата', key: 'creationDate' },
  { title: 'Оскары', key: 'oscarsCount' },
  { title: 'Жанр', key: 'genre' },
  { title: 'Возраст', key: 'mpaaRating' },
  // { title: 'Coord.ID', key: 'coordinates.id' },
  { title: 'Х', key: 'coordinates.x' },
  { title: 'Y', key: 'coordinates.y' },
  // { title: 'Oper.ID', key: 'operator.id' },
  { title: 'Имя режиссера', key: 'operator.name' },
  { title: 'Рост режиссера', key: 'operator.height' },
  { title: 'Вест реижиссера', key: 'operator.weight' },
  // { title: 'Loc.ID', key: 'operator.location.id' },
  { title: 'Локация Х', key: 'operator.location.x' },
  { title: 'Локация Y', key: 'operator.location.y' },
  { title: 'Локация Z', key: 'operator.location.z' },
]

const GENRES = ['Драма', 'Фэнтези', 'Триллер']
const MPAA = ['PG', 'R', 'NC_17']
const INT_MAX = 2147483647
const INT_MAX_MINUS_ONE = INT_MAX - 1 // 2147483646
const INT_MIN = -2147483648
const GENRE_LABELS = {
  DRAMA: 'Драма',
  FANTASY: 'Фэнтези',
  THRILLER: 'Триллер',
}

const GENRE_OPTIONS = Object.entries(GENRE_LABELS).map(([value, label]) => ({ value, label }))

function genreLabel(v) {
  return GENRE_LABELS[v] ?? v
}
function inRange(n, min, max) {
  if (n === null || n === undefined || Number.isNaN(n)) return false
  return n >= min && n <= max
}
function inRangeIntMinusOne(n) {
  return inRange(n, INT_MIN, INT_MAX_MINUS_ONE)
}
function inRangeNonNegativeIntMinusOne(n) {
  return inRange(n, 0, INT_MAX_MINUS_ONE)
}
function inRangeFloatMinusOne(n) {
  return inRange(n, -Number(INT_MAX_MINUS_ONE), Number(INT_MAX_MINUS_ONE))
}

function clamp(n, min, max) {
  if (n === null || n === undefined || Number.isNaN(n)) return n
  return Math.min(max, Math.max(min, n))
}
function clampPayload(p) {
  const out = deepClone(p)
  out.coordinates.x = clamp(Number(out.coordinates.x), INT_MIN, INT_MAX_MINUS_ONE)
  out.coordinates.y = clamp(Number(out.coordinates.y), -INT_MAX_MINUS_ONE, INT_MAX_MINUS_ONE)
  out.oscarsCount = clamp(Number(out.oscarsCount), 1, INT_MAX_MINUS_ONE)
  out.operator.height = clamp(Number(out.operator.height), 1, INT_MAX_MINUS_ONE)
  out.operator.weight = clamp(Number(out.operator.weight), 1, INT_MAX_MINUS_ONE)
  out.operator.location.x = clamp(Number(out.operator.location.x), INT_MIN, INT_MAX_MINUS_ONE)
  out.operator.location.y = clamp(Number(out.operator.location.y), INT_MIN, INT_MAX_MINUS_ONE)
  out.operator.location.z = clamp(Number(out.operator.location.z), INT_MIN, INT_MAX_MINUS_ONE)
  return out
}
const FIELDS = [
  { key: 'id', label: 'ID', type: 'number' },
  { key: 'name', label: 'Название', type: 'string' },
  { key: 'coordinates.x', label: 'Coordinates X', type: 'number' },
  { key: 'coordinates.y', label: 'Coordinates Y', type: 'number' },
  { key: 'creationDate', label: 'Дата создания', type: 'date' },
  { key: 'oscarsCount', label: 'Оскары', type: 'number' },
  { key: 'genre', label: 'Жанр', type: 'enum', options: Object.keys(GENRE_LABELS) },
  { key: 'mpaaRating', label: 'MPAA', type: 'enum', options: MPAA },
  { key: 'operator.name', label: 'Оператор — имя', type: 'string' },
  { key: 'operator.height', label: 'Оператор — рост', type: 'number' },
  { key: 'operator.weight', label: 'Оператор — вес', type: 'number' },
  { key: 'operator.location.x', label: 'Локация X', type: 'number' },
  { key: 'operator.location.y', label: 'Локация Y', type: 'number' },
  { key: 'operator.location.z', label: 'Локация Z', type: 'number' },
]

const OP_LABELS = { eq: 'равно', ne: 'не равно', gt: '>', gte: '≥', lt: '<', lte: '≤' }
const OPS_BY_TYPE = {
  string: ['eq', 'ne'],
  enum: ['eq', 'ne'],
  number: ['eq', 'ne', 'gt', 'gte', 'lt', 'lte'],
  date: ['eq', 'ne', 'gt', 'gte', 'lt', 'lte'],
}

const page = ref(0)
const size = ref(20)
const sort = ref('id:desc')
const loading = ref(false)
const error = ref('')
const rows = ref([])
const totalElements = ref(0)
const totalPages = ref(1)

const sortKey = computed(() => sort.value.split(':')[0])
const sortDir = computed(() => sort.value.split(':')[1] || 'asc')
const isCreateMode = computed(() => !editId.value)
function openCreate() {
  editId.value = null
  editForm.value = makeEmptyForm()
  editError.value = ''
  editing.value = true
}

const isFormValid = computed(() => {
  const f = editForm.value
  if (!f) return false

  if (!f.name || !f.name.trim()) return false

  if (f.coordinates.x == null || f.coordinates.y == null) return false

  if (!Number.isInteger(f.coordinates.x)) return false
  if (normalizeNumber(f.coordinates.x) === false) return false
  if (normalizeNumber(f.coordinates.y) === false) return false

  if (!inRangeIntMinusOne(f.coordinates.x)) return false
  if (!inRangeFloatMinusOne(f.coordinates.y)) return false

  if (!Number.isInteger(f.oscarsCount) || f.oscarsCount <= 0) return false
  if (!inRangeNonNegativeIntMinusOne(f.oscarsCount)) return false

  if (!f.genre || !f.mpaaRating) return false

  const op = f.operator
  if (op) {
    if (!op.name || !op.name.trim()) return false

    if (op.height < 0) return false
    if (!inRangeNonNegativeIntMinusOne(op.height)) return false

    if (!Number.isInteger(op.weight) || op.weight < 0) return false
    if (!inRangeNonNegativeIntMinusOne(op.weight)) return false

    if (op.location) {
      const loc = op.location
      if (loc.x == null || loc.y == null || loc.z == null) return false

      if (!Number.isInteger(loc.x) || !inRangeIntMinusOne(loc.x)) return false
      if (!Number.isInteger(loc.y) || !inRangeIntMinusOne(loc.y)) return false
      if (!Number.isInteger(loc.z) || !inRangeIntMinusOne(loc.z)) return false

      if (normalizeNumber(loc.x) === false) return false
      if (normalizeNumber(loc.y) === false) return false
      if (normalizeNumber(loc.z) === false) return false
    } else return false
  }
  return true
})

function trimDecimalWithCaret(e, max = 6) {
  const el = e.target
  let start = el.selectionStart ?? el.value.length
  let end = el.selectionEnd ?? el.value.length
  let v = String(el.value ?? '')

  // Оставляем цифры, точку/запятую и один минус
  v = v.replace(/[^\d.,-]/g, '')
  // Минус только в начале
  v = v.replace(/(?!^)-/g, '')

  // Первый разделитель дроби — точка или запятая
  const firstSepMatch = /[.,]/.exec(v)
  const sepIndex = firstSepMatch ? firstSepMatch.index : -1

  if (sepIndex !== -1) {
    // Удаляем все последующие разделители после первого
    const before = v.slice(0, sepIndex + 1)
    const afterOrig = v.slice(sepIndex + 1)
    const after = afterOrig.replace(/[.,]/g, '') // только цифры после разделителя

    // Корректируем позицию каретки, если что-то удалили после разделителя
    const removedSeps = (afterOrig.match(/[.,]/g) || []).length
    if (removedSeps && start > sepIndex) {
      start -= Math.min(removedSeps, start - (sepIndex + 1))
      end -= Math.min(removedSeps, end - (sepIndex + 1))
    }

    // Режем дробную часть до max
    let frac = after
    if (after.length > max) {
      const cut = after.length - max
      frac = after.slice(0, max)

      // Если каретка была внутри «срезанной» области — сдвигаем
      if (start > sepIndex + 1 + max) start -= cut
      if (end > sepIndex + 1 + max) end -= cut
    }

    v = before + frac
  }

  // Обновляем DOM только если строка изменилась (меньше мерцаний и прыжков)
  if (el.value !== v) {
    el.value = v
    // Вернуть каретку после перерисовки
    requestAnimationFrame(() => el.setSelectionRange(start, end))
  }

  // В модель кладём число (или пустую строку, если невалидно)
  const n = Number(v.replace(',', '.'))
  return Number.isFinite(n) ? n : ''
}

function formatDate(val) {
  if (!val) return '—'
  // поддержка ISO-строки или Date
  const d = new Date(val)
  if (isNaN(d.getTime())) return val // если не дата — вернуть как есть
  const dd = String(d.getDate()).padStart(2, '0')
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const yyyy = d.getFullYear()
  return `${dd}.${mm}.${yyyy}`
}

function onIntegerInput(e, path) {
  const el = e.target

  // если пользователь ввёл точку/запятую — сразу подсветим и не трогаем модель
  if (/[.,]/.test(el.value)) {
    el.classList.add('invalid')
    return
  }

  // жёстко клампим визуально под int32 диапазон
  const clampedStr = clampIntStringInPlace(el)
  const n = Number(clampedStr)

  // обновляем реактивную модель
  const keys = path.split('.')
  let obj = editForm.value
  for (let i = 0; i < keys.length - 1; i++) obj = obj[keys[i]]
  obj[keys[keys.length - 1]] = Number.isFinite(n) ? n : ''

  // убираем ручную подсветку, дальнейшая валидация — через ваши правила
  el.classList.remove('invalid')
}

function makeEmptyForm() {
  return {
    name: '',
    id: 999,
    coordinates: { x: 0, y: 0 },
    oscarsCount: 0,
    genre: GENRES[0],
    mpaaRating: MPAA[0],
    operator: {
      name: '',
      height: 0,
      weight: 0,
      location: { x: 0, y: 0, z: 0 },
    },
  }
}
const deletingId = ref(null)
const editing = ref(false)
const saving = ref(false)
const editError = ref('')
const editId = ref(null)
const editForm = ref(makeEmptyForm())
function deepClone(o) {
  return JSON.parse(JSON.stringify(o))
}

function openEdit(movie) {
  editId.value = movie.id
  editForm.value = deepClone({
    name: movie.name ?? '',
    coordinates: {
      x: Number(getVal(movie, 'coordinates.x') ?? 0),
      y: Number(getVal(movie, 'coordinates.y') ?? 0),
    },
    oscarsCount: Number(movie.oscarsCount ?? 0),
    genre: movie.genre ?? GENRES[0],
    mpaaRating: movie.mpaaRating ?? MPAA[0],
    operator: {
      name: getVal(movie, 'operator.name') ?? '',
      height: Number(getVal(movie, 'operator.height') ?? 0),
      weight: Number(getVal(movie, 'operator.weight') ?? 0),
      location: {
        x: Number(getVal(movie, 'operator.location.x') ?? 0),
        y: Number(getVal(movie, 'operator.location.y') ?? 0),
        z: Number(getVal(movie, 'operator.location.z') ?? 0),
      },
    },
  })
  editError.value = ''
  editing.value = true
}

function cancelEdit() {
  if (saving.value) return
  editing.value = false
  editId.value = null
  editForm.value = makeEmptyForm()
  editError.value = ''
}
function normalizeNumber(value) {
  if (value === null || value === undefined || value === '') return false
  const str = String(value).trim().replace(',', '.')
  const num = Number(str)
  if (isNaN(num)) return false
  const parts = str.split('.')
  if (parts.length === 1) return true
  return parts[1].length <= 6
}

function trimDecimalsInEvent(e, max = 6) {
  // нормализуем разделитель
  let v = String(e.target.value ?? '').replace(',', '.')
  // допускаем только один минус в начале и одну точку
  // (мягко: не вычищаем всё, а только режем дробную часть)
  const dotIdx = v.indexOf('.')
  if (dotIdx !== -1) {
    const intPart = v.slice(0, dotIdx)
    const fracPart = v.slice(dotIdx + 1)
    if (fracPart.length > max) {
      v = intPart + '.' + fracPart.slice(0, max)
    }
  }
  e.target.value = v // визуально «съедаем» лишние символы сразу

  // Возвращаем значение для модели:
  // если пусто/только "-" или ".", оставим пустую строку — пусть форма помечается как невалидная
  if (v === '' || v === '-' || v === '.' || v === '-.') return ''
  const n = Number(v)
  return Number.isNaN(n) ? '' : n
}

function onCoordYInput(e) {
  const n = trimDecimalsInEvent(e, 6)
  editForm.value.coordinates.y = n
}
function onHeightInput(e) {
  const n = trimDecimalsInEvent(e, 6)
  editForm.value.operator.height = n
}

async function saveEdit() {
  saving.value = true
  editError.value = ''
  try {
    const payload = deepClone(editForm.value)
    if (isCreateMode.value) {
      console.log('fff' + JSON.stringify(payload))
      await createMovie(payload)
    } else {
      await updateMovie(editId.value, payload)
    }
    await load()
    editing.value = false
    editId.value = null
    editForm.value = makeEmptyForm()
    editError.value = ''
  } catch (e) {
    editError.value = e?.response?.data?.message || 'Не удалось сохранить изменения'
  } finally {
    saving.value = false
  }
}

function thClass(key) {
  return {
    sortable: true,
    active: sortKey.value === key,
    asc: sortKey.value === key && sortDir.value === 'asc',
    desc: sortKey.value === key && sortDir.value === 'desc',
  }
}
function toggleSort(key) {
  const same = sortKey.value === key
  sort.value = same ? `${key}:${sortDir.value === 'asc' ? 'desc' : 'asc'}` : `${key}:asc`
  page.value = 0
  load()
}
function onSizeChange() {
  page.value = 0
  load()
}
function goTo(p) {
  page.value = Math.max(0, p)
  load()
}

let nextId = 1
const filters = ref([])
function addFilter() {
  const fld = FIELDS[0]
  filters.value.push({
    id: nextId++,
    field: fld.key,
    op: OPS_BY_TYPE[fld.type][0],
    value: '',
    values: [],
  })
}
function removeFilter(idx) {
  filters.value.splice(idx, 1)
}
function clearFilters() {
  filters.value = []
  page.value = 0
  load()
}
function applyFilters() {
  page.value = 0
  load()
}
function fieldMeta(fieldKey) {
  return FIELDS.find((f) => f.key === fieldKey) || { type: 'string' }
}
function opsFor(fieldKey) {
  return OPS_BY_TYPE[fieldMeta(fieldKey).type] || ['eq', 'ne']
}
function enumOptionsFor(fieldKey) {
  return fieldMeta(fieldKey).options || []
}
function isNumberField(fieldKey) {
  return fieldMeta(fieldKey).type === 'number'
}

function inputKind(f) {
  const meta = fieldMeta(f.field)
  if (meta.type === 'enum') return f.op === 'in' ? 'enum-multi' : 'enum'
  return meta.type
}
function onFieldChange(f) {
  const allowed = opsFor(f.field)
  if (!allowed.includes(f.op)) f.op = allowed[0]
  f.value = ''
  f.values = []
}

function getVal(obj, path) {
  return path.split('.').reduce((acc, k) => (acc != null ? acc[k] : undefined), obj)
}
function formatValue(v, key) {
  if (v === null || v === undefined) return '—'
  if (key === 'genre') return genreLabel(v)
  if (key === 'creationDate') return formatDate(v)
  return v
}

function isFloatField(fieldKey) {
  // у нас во входной форме флоатами были coordinates.y и operator.height
  return fieldKey === 'coordinates.y' || fieldKey === 'operator.height'
}
function isIntField(fieldKey) {
  return fieldMeta(fieldKey).type === 'number' && !isFloatField(fieldKey)
}

function clampIntStringInPlace(el) {
  // допустимые границы как строки без знака
  const MAX_POS_STR = String(INT_MAX_MINUS_ONE) // "2147483646"
  const MAX_NEG_STR = String(Math.abs(INT_MIN)) // "2147483648"

  // сырое значение
  let v = String(el.value ?? '')

  // разрешаем только цифры и один минус в начале
  v = v.replace(/[^\d-]/g, '').replace(/(?!^)-/g, '')

  const neg = v.startsWith('-')
  let digits = v.replace(/^-/, '')

  // убираем лидирующие нули (оставим один ноль, если все нули)
  digits = digits.replace(/^0+(?=\d)/, '')
  if (digits === '') digits = '0'

  // ограничиваем по длине и лексикографически
  const limit = neg ? MAX_NEG_STR : MAX_POS_STR

  if (digits.length > limit.length) {
    digits = limit
  } else if (digits.length === limit.length && digits > limit) {
    digits = limit
  }

  const out = (neg ? '-' : '') + digits

  if (el.value !== out) {
    el.value = out
  }
  return out
}

function clampInt(n) {
  return Math.min(INT_MAX_MINUS_ONE, Math.max(INT_MIN, n))
}
function clampFloat(n) {
  const lim = Number(INT_MAX_MINUS_ONE)
  return Math.min(lim, Math.max(-lim, n))
}
function trimFloatStringKeepCaret(el, maxFrac = 6) {
  // не усложняем каретку в фильтрах: просто подрезаем
  let v = String(el.value ?? '')
    .replace(',', '.')
    .replace(/[^\d\.\-]/g, '')
  v = v.replace(/(?!^)-/g, '') // минус только в начале
  v = v.replace(/\.(?=.*\.)/g, '') // только одна точка
  const dot = v.indexOf('.')
  if (dot !== -1)
    v =
      v.slice(0, dot + 1) +
      v
        .slice(dot + 1)
        .replace(/\D/g, '')
        .slice(0, maxFrac)
  el.value = v
  return v
}

// одиночное значение для number-поля
function onFilterNumberInput(e, f) {
  const meta = fieldMeta(f.field)
  if (meta.type !== 'number') return

  if (isFloatField(f.field)) {
    const str = trimFloatStringKeepCaret(e.target, 6)
    const n = Number(str)
    const valid = str !== '' && Number.isFinite(n)
    f._invalid = !valid || !inRangeFloatMinusOne(n)
    f.value = valid ? clampFloat(n) : str // хранить число или исходную строку, если невалидно
  } else {
    // int
    let v = String(e.target.value ?? '')
    // если есть запятая/точка — сразу невалидно и не пишем в модель
    if (/[.,]/.test(v)) {
      f._invalid = true
      return
    }
    // оставляем цифры и опциональный ведущий минус
    v = v.replace(/[^\d-]/g, '').replace(/(?!^)-/g, '')
    e.target.value = v
    const n = Number(v)
    const isNum = v !== '' && Number.isFinite(n)
    const clamped = isNum ? clampInt(n) : n
    f._invalid = !isNum || !inRangeIntMinusOne(clamped)
    f.value = isNum ? clamped : v
  }
}

// CSV-режим для number-поля (op === 'in')
function onFilterNumberCsvInput(e, f) {
  const raw = String(e.target.value ?? '')
  // не трогаем визуально, но валидируем каждую часть
  const parts = raw
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)

  let invalid = false
  const nums = parts.map((p) => {
    if (isFloatField(f.field)) {
      // флоат: до 6 знаков
      const cleaned = p.replace(',', '.')
      const dot = cleaned.indexOf('.')
      const limited =
        dot === -1 ? cleaned : cleaned.slice(0, dot + 1) + cleaned.slice(dot + 1).slice(0, 6)
      const n = Number(limited)
      if (limited === '' || !Number.isFinite(n) || !inRangeFloatMinusOne(n)) invalid = true
      return clampFloat(n)
    } else {
      // инт: запрещаем , .
      if (/[.,]/.test(p)) invalid = true
      const cleaned = p.replace(/[^\d-]/g, '').replace(/(?!^)-/g, '')
      const n = Number(cleaned)
      if (cleaned === '' || !Number.isFinite(n) || !inRangeIntMinusOne(n)) invalid = true
      return clampInt(n)
    }
  })

  f._invalid = invalid
  // храним строку для UX, но при отправке (buildFiltersObject) превратим в числа
  f.value = raw
}

function parseCsv(val, fieldKey) {
  const parts = String(val)
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
  const meta = fieldMeta(fieldKey)
  return meta.type === 'number' ? parts.map(Number) : parts
}

function buildFiltersObject() {
  const out = {}
  for (const f of filters.value) {
    const key = `${f.field}[${f.op}]`
    const meta = fieldMeta(f.field)
    let val
    if (f.op === 'in') {
      val =
        meta.type === 'enum'
          ? Array.isArray(f.values)
            ? f.values
            : []
          : // number CSV -> массив чисел с нужной интерпретацией
            String(f.value ?? '')
              .split(',')
              .map((s) => s.trim())
              .filter(Boolean)
              .map((x) =>
                isFloatField(f.field)
                  ? clampFloat(Number(x.replace(',', '.')))
                  : clampInt(Number(x)),
              )
    } else {
      val =
        meta.type === 'number'
          ? f.value === '' || f.value === null
            ? null
            : isFloatField(f.field)
              ? clampFloat(Number(f.value))
              : clampInt(Number(f.value))
          : f.value
    }
    const isEmpty =
      (f.op === 'in' && Array.isArray(val) && val.length === 0) ||
      (f.op !== 'in' && (val === '' || val == null))
    if (!isEmpty) out[key] = val
  }
  return out
}

function buildBody() {
  return {
    filters: buildFiltersObject(),
    sort: [sort.value],
    page: page.value,
    size: size.value,
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await searchMovies(buildBody())
    rows.value = data.content ?? []
    totalElements.value = data.totalElements ?? 0
    totalPages.value = data.totalPages ?? 1
  } catch (e) {
    error.value = e?.response?.data?.message || 'Не удалось загрузить фильмы'
    rows.value = []
    totalElements.value = 0
    totalPages.value = 1
  } finally {
    loading.value = false
  }
}

const fromGenre = ref('')
const toGenre = ref('')
const transferLoading = ref(false)
const transferResult = ref(0)
const transferError = ref('')

const canTransfer = computed(
  () => !!fromGenre.value && !!toGenre.value && fromGenre.value !== toGenre.value,
)

const areFiltersValid = computed(() => {
  for (const f of filters.value) {
    const meta = fieldMeta(f.field)
    if (f._invalid) return false

    // пустые — не считаем ошибкой: они всё равно не попадут в body
    const hasVal =
      f.op === 'in'
        ? (Array.isArray(f.values) && f.values.length > 0) ||
          (typeof f.value === 'string' && f.value.trim() !== '')
        : f.value !== '' && f.value != null

    // если задано значение и тип number — убеждаемся, что это действительно число
    if (hasVal && meta.type === 'number' && f.op !== 'in') {
      if (typeof f.value !== 'number' || Number.isNaN(f.value)) return false
    }
    // CSV для number — проверим, что все токены валидны
    if (hasVal && meta.type === 'number' && f.op === 'in') {
      const tokens = String(f.value)
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean)
      if (tokens.length === 0) return false
      for (const t of tokens) {
        if (isFloatField(f.field)) {
          const n = Number(t.replace(',', '.'))
          if (!Number.isFinite(n) || !inRangeFloatMinusOne(n)) return false
        } else {
          if (/[.,]/.test(t)) return false
          const n = Number(t)
          if (!Number.isFinite(n) || !Number.isInteger(n) || !inRangeIntMinusOne(n)) return false
        }
      }
    }
  }
  return true
})

async function onTransfer() {
  if (!canTransfer.value) return
  transferError.value = ''
  transferResult.value = 0
  transferLoading.value = true
  try {
    const res = await redistributeRewards(fromGenre.value, toGenre.value)

    transferResult.value = res?.transferredCount ?? 0

    await load()
  } catch (e) {
    transferError.value = e?.response?.data?.message || 'Не удалось перераспределить награды'
  } finally {
    transferLoading.value = false
  }
}

async function onDelete(movie) {
  if (deletingId.value) return
  deletingId.value = movie.id
  try {
    await deleteMovie(movie.id)
    await load()
  } catch (e) {
    console.error(e)
    alert('Не удалось удалить фильм')
  } finally {
    if (deletingId.value === movie.id) deletingId.value = null
  }
}

onMounted(load)
</script>

<style scoped>
.page {
  padding: 24px;
  font-family:
    system-ui,
    -apple-system,
    Segoe UI,
    Roboto,
    Arial,
    sans-serif;
  color: #0b3ba7;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.right {
  display: flex;
  gap: 12px;
  align-items: center;
}
.size select {
  margin-left: 6px;
}

.card {
  background: #fff;
  border: 1px solid #e6edff;
  border-radius: 14px;
  box-shadow: 0 8px 24px rgba(20, 60, 200, 0.05);
}
.transfer-card {
  padding: 12px;
  margin-bottom: 12px;
}
.transfer-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.transfer-title {
  font-weight: 700;
}
.transfer-controls {
  display: flex;
  gap: 10px;
  align-items: center;
}
.inline {
  display: flex;
  gap: 6px;
  align-items: center;
}

.filters {
  padding: 12px;
  margin-bottom: 12px;
}
.filters-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.filters-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.filter-rows {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.filter-row {
  display: grid;
  grid-template-columns: 1.2fr 0.9fr 2fr auto;
  gap: 8px;
  align-items: center;
}
.hint {
  margin: 8px 0 0;
  color: #5a77c7;
  font-size: 13px;
}

.input,
.select {
  padding: 8px 10px;
  border: 1px solid #cfe0ff;
  border-radius: 8px;
  outline: none;
  background: #fff;
  color: #0b3ba7;
}
.input:focus,
.select:focus {
  border-color: #2f5eea;
}

.btn {
  padding: 8px 12px;
  background: #2f5eea;
  color: #fff;
  border: 0;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
}
.link {
  border: 0;
  background: transparent;
  color: #1f56ff;
  text-decoration: underline;
  cursor: pointer;
  padding: 0;
}
.link.danger {
  color: #c62828;
  text-decoration: none;
}

.table {
  width: 100%;
  border-collapse: collapse;
  min-width: 1100px;
}
.table th,
.table td {
  padding: 10px 12px;
  border-bottom: 1px solid #eef2ff;
  white-space: nowrap;
}
.table thead th {
  background: #f5f8ff;
  color: #0b3ba7;
  user-select: none;
  cursor: pointer;
}
.sortable.active.asc::after {
  content: ' ▲';
  color: #2f5eea;
}
.sortable.active.desc::after {
  content: ' ▼';
  color: #2f5eea;
}

.loading {
  padding: 12px;
  color: #1b3f9e;
}
.error {
  padding: 12px;
  color: #c62828;
}
.success {
  padding: 8px 0 0;
  color: #087f23;
}

.empty {
  text-align: center;
  color: #5a77c7;
}
.pager {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  color: #0b3ba7;
}
.pg-btn {
  border: 1px solid #cfe0ff;
  background: #fff;
  color: #1f56ff;
  border-radius: 8px;
  padding: 6px 10px;
  cursor: pointer;
}
.pg-btn[disabled] {
  opacity: 0.5;
  cursor: default;
}
.pg-info {
  color: #0b3ba7;
}
.actions-col {
  width: 110px;
  text-align: right;
}
.actions {
  text-align: right;
}
.trash-btn {
  border: 0;
  background: #ffeded;
  color: #c01818;
  padding: 6px 10px;
  border-radius: 8px;
  cursor: pointer;
}
.trash-btn[disabled] {
  opacity: 0.5;
  cursor: default;
}
.trash-btn:hover:not([disabled]) {
  background: #ffdcdc;
}

/* Кнопка редактирования рядом с корзиной */
.edit-btn {
  border: 0;
  background: #eef6ff;
  color: #0b3ba7;
  padding: 6px 10px;
  border-radius: 8px;
  cursor: pointer;
  margin-right: 6px;
}
.edit-btn[disabled] {
  opacity: 0.5;
  cursor: default;
}
.edit-btn:hover:not([disabled]) {
  background: #dcebff;
}

/* Модалка */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(17, 27, 53, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  z-index: 50;
}
.invalid {
  border-color: #d32f2f;
  background: #fff5f5;
}
.btn:disabled {
  background: #cfd6e6; /* светло-серая заливка */
  color: #7a8194; /* серый текст */
  cursor: not-allowed;
  border: 0;
  opacity: 1; /* чтобы не бледнело ещё сильнее */
}
.modal {
  background: #fff;
  border-radius: 12px;
  width: 720px;
  max-width: 100%;
  padding: 16px;
  box-shadow: 0 8px 24px rgba(10, 20, 60, 0.2);
}
.form {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}
.two {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.three {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 10px;
}
.form label {
  display: grid;
  gap: 6px;
}
.form input,
.form select {
  height: 36px;
  padding: 6px 10px;
  border: 1px solid #dbe6ff;
  border-radius: 8px;
}
.fieldset {
  border: 1px dashed #dbe6ff;
  padding: 12px;
  border-radius: 8px;
}
.fieldset legend {
  padding: 0 6px;
  color: #0b3ba7;
}
.modal-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
