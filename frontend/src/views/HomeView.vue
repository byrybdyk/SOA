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

    <!-- 🟦 Перераспределение наград -->
    <div class="card transfer-card">
      <div class="transfer-row">
        <div class="transfer-title">Перераспределить «Оскары» между жанрами</div>
        <div class="transfer-controls">
          <label class="inline">
            Из жанра:
            <select class="input select" v-model="fromGenre">
              <option disabled value="">Выберите жанр</option>
              <option v-for="g in GENRES" :key="g" :value="g">{{ g }}</option>
            </select>
          </label>
          <label class="inline">
            В жанр:
            <select class="input select" v-model="toGenre">
              <option disabled value="">Выберите жанр</option>
              <option v-for="g in GENRES" :key="g" :value="g">{{ g }}</option>
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

    <!-- ФИЛЬТРЫ -->
    <div class="filters card">
      <div class="filters-header">
        <strong>Фильтры</strong>
        <div class="filters-actions">
          <button class="link" @click="addFilter">+ условие</button>
          <button class="link danger" @click="clearFilters" :disabled="filters.length === 0">
            Очистить
          </button>
          <button class="btn" @click="applyFilters">Применить</button>
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
                {{ opt }}
              </option>
            </select>
          </template>

          <template v-else-if="inputKind(f) === 'enum'">
            <select class="input select" v-model="f.value">
              <option value="" disabled>Выберите…</option>
              <option v-for="opt in enumOptionsFor(f.field)" :key="opt" :value="opt">
                {{ opt }}
              </option>
            </select>
          </template>

          <template v-else-if="inputKind(f) === 'date'">
            <input class="input" type="date" v-model="f.value" />
          </template>

          <template v-else>
            <input
              class="input"
              :type="isNumberField(f.field) ? 'number' : 'text'"
              v-model="f.value"
              :placeholder="f.op === 'in' ? 'Через запятую: a,b,c' : 'Значение'"
            />
          </template>

          <button class="link danger" @click="removeFilter(idx)">Удалить</button>
        </div>
        <p class="hint">Поддерживаются операторы: =, ≠, &gt;, ≥, &lt;, ≤, IN.</p>
      </div>
    </div>

    <!-- ТАБЛИЦА -->
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
          </tr>
        </thead>
        <tbody>
          <tr v-for="m in rows" :key="m.id">
            <td v-for="col in COLUMNS" :key="col.key">
              {{ formatValue(getVal(m, col.key)) }}
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

    <!-- ПАГИНАЦИЯ -->
    <div class="pager" v-if="totalPages > 1">
      <button class="pg-btn" :disabled="page === 0" @click="goTo(page - 1)">‹</button>
      <span class="pg-info">Стр. {{ page + 1 }} из {{ totalPages }}</span>
      <button class="pg-btn" :disabled="page >= totalPages - 1" @click="goTo(page + 1)">›</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { searchMovies } from '../api/movies'
import { redistributeRewards } from '../api/genres'

// ---- Табличные колонки (все поля, в т.ч. вложенные)
const COLUMNS = [
  { title: 'ID', key: 'id' },
  { title: 'Название', key: 'name' },
  { title: 'Дата', key: 'creationDate' },
  { title: 'Оскары', key: 'oscarsCount' },
  { title: 'Жанр', key: 'genre' },
  { title: 'MPAA', key: 'mpaaRating' },
  { title: 'Coord.ID', key: 'coordinates.id' },
  { title: 'Coord.X', key: 'coordinates.x' },
  { title: 'Coord.Y', key: 'coordinates.y' },
  { title: 'Oper.ID', key: 'operator.id' },
  { title: 'Oper.Name', key: 'operator.name' },
  { title: 'Oper.Height', key: 'operator.height' },
  { title: 'Oper.Weight', key: 'operator.weight' },
  { title: 'Loc.ID', key: 'operator.location.id' },
  { title: 'Loc.X', key: 'operator.location.x' },
  { title: 'Loc.Y', key: 'operator.location.y' },
  { title: 'Loc.Z', key: 'operator.location.z' },
]

// ---- Справочники
const GENRES = ['DRAMA', 'FANTASY', 'THRILLER']
const MPAA = ['G', 'PG', 'PG_13', 'R', 'NC_17']

// ---- Фильтрация
const FIELDS = [
  { key: 'id', label: 'ID', type: 'number' },
  { key: 'name', label: 'Название', type: 'string' },
  { key: 'coordinates.x', label: 'Coordinates X', type: 'number' },
  { key: 'coordinates.y', label: 'Coordinates Y', type: 'number' },
  { key: 'creationDate', label: 'Дата создания', type: 'date' },
  { key: 'oscarsCount', label: 'Оскары', type: 'number' },
  { key: 'genre', label: 'Жанр', type: 'enum', options: GENRES },
  { key: 'mpaaRating', label: 'MPAA', type: 'enum', options: MPAA },
  { key: 'operator.name', label: 'Оператор — имя', type: 'string' },
  { key: 'operator.height', label: 'Оператор — рост', type: 'number' },
  { key: 'operator.weight', label: 'Оператор — вес', type: 'number' },
  { key: 'operator.location.x', label: 'Локация X', type: 'number' },
  { key: 'operator.location.y', label: 'Локация Y', type: 'number' },
  { key: 'operator.location.z', label: 'Локация Z', type: 'number' },
]

const OP_LABELS = { eq: 'равно', ne: 'не равно', gt: '>', gte: '≥', lt: '<', lte: '≤', in: 'IN' }
const OPS_BY_TYPE = {
  string: ['eq', 'ne', 'in'],
  enum: ['eq', 'ne', 'in'],
  number: ['eq', 'ne', 'gt', 'gte', 'lt', 'lte', 'in'],
  date: ['eq', 'ne', 'gt', 'gte', 'lt', 'lte', 'in'],
}

// ---- Таблица: состояние
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

// ---- Фильтры-конструктор
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

// ---- helpers
function getVal(obj, path) {
  return path.split('.').reduce((acc, k) => (acc != null ? acc[k] : undefined), obj)
}
function formatValue(v) {
  if (v === null || v === undefined) return '—'
  return v
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
          : Array.isArray(f.value)
            ? f.value
            : parseCsv(f.value, f.field)
    } else {
      val =
        meta.type === 'number'
          ? f.value === '' || f.value === null
            ? null
            : Number(f.value)
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

// 🟦 Перераспределение: состояние и логика
const fromGenre = ref('')
const toGenre = ref('')
const transferLoading = ref(false)
const transferResult = ref(0)
const transferError = ref('')

const canTransfer = computed(
  () => !!fromGenre.value && !!toGenre.value && fromGenre.value !== toGenre.value,
)

async function onTransfer() {
  if (!canTransfer.value) return
  transferError.value = ''
  transferResult.value = 0
  transferLoading.value = true
  try {
    const res = await redistributeRewards(fromGenre.value, toGenre.value)
    // a) показать результат
    transferResult.value = res?.transferredCount ?? 0
    // б) обновить таблицу, НЕ сбрасывая фильтры/сортировку/страницу
    await load()
  } catch (e) {
    transferError.value = e?.response?.data?.message || 'Не удалось перераспределить награды'
  } finally {
    transferLoading.value = false
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
</style>
