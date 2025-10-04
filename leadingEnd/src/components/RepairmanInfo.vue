<template>
  <div class="repairman-info">
    <div class="module-header">
      <h2>检修员信息管理</h2>
      <div class="header-actions">
        <button class="btn btn-primary" @click="showAddDialog = true">
          <i class="icon-plus"></i>添加检修员
        </button>
        <button class="btn btn-success" @click="exportToExcel">
          <i class="icon-export"></i>导出Excel
        </button>
      </div>
    </div>

    <!-- 搜索区域 -->
    <div class="search-section">
      <div class="search-row">
        <div class="search-item">
          <label>搜索字段：</label>
          <select v-model="searchField" class="search-select">
            <option value="">请选择搜索字段</option>
            <option value="id">检修员ID</option>
            <option value="name">姓名</option>
            <option value="phone">联系方式</option>
            <option value="area_name">所属区域</option>
          </select>
        </div>
        <div class="search-item">
          <label>关键词：</label>
          <input 
            v-model="searchKeyword" 
            type="text" 
            placeholder="请输入搜索关键词"
            class="search-input"
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="search-item">
          <label>性别：</label>
          <select v-model="sexFilter" class="search-select">
            <option value="">全部</option>
            <option value="0">女</option>
            <option value="1">男</option>
          </select>
        </div>
        <div class="search-item">
          <label>年龄范围：</label>
          <input v-model="minAge" type="number" placeholder="最小年龄" class="search-input age-input" />
          <span>-</span>
          <input v-model="maxAge" type="number" placeholder="最大年龄" class="search-input age-input" />
        </div>
        <button class="btn btn-primary" @click="handleSearch">搜索</button>
        <button class="btn btn-secondary" @click="resetSearch">重置</button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th @click="handleSort('id')" class="sortable">
              检修员ID
              <span class="sort-indicator" v-if="sortField === 'id'">
                {{ sortOrder === 'asc' ? '↑' : '↓' }}
              </span>
            </th>
            <th @click="handleSort('name')" class="sortable">
              姓名
              <span class="sort-indicator" v-if="sortField === 'name'">
                {{ sortOrder === 'asc' ? '↑' : '↓' }}
              </span>
            </th>
            <th @click="handleSort('age')" class="sortable">
              年龄
              <span class="sort-indicator" v-if="sortField === 'age'">
                {{ sortOrder === 'asc' ? '↑' : '↓' }}
              </span>
            </th>
            <th @click="handleSort('sex')" class="sortable">
              性别
              <span class="sort-indicator" v-if="sortField === 'sex'">
                {{ sortOrder === 'asc' ? '↑' : '↓' }}
              </span>
            </th>
            <th @click="handleSort('phone')" class="sortable">
              联系方式
              <span class="sort-indicator" v-if="sortField === 'phone'">
                {{ sortOrder === 'asc' ? '↑' : '↓' }}
              </span>
            </th>
            <th @click="handleSort('entry_time')" class="sortable">
              入职时间
              <span class="sort-indicator" v-if="sortField === 'entry_time'">
                {{ sortOrder === 'asc' ? '↑' : '↓' }}
              </span>
            </th>
            <th @click="handleSort('area_name')" class="sortable">
              所属区域
              <span class="sort-indicator" v-if="sortField === 'area_name'">
                {{ sortOrder === 'asc' ? '↑' : '↓' }}
              </span>
            </th>
            <th @click="handleSort('create_time')" class="sortable">
              创建时间
              <span class="sort-indicator" v-if="sortField === 'create_time'">
                {{ sortOrder === 'asc' ? '↑' : '↓' }}
              </span>
            </th>
            <th @click="handleSort('update_time')" class="sortable">
              更新时间
              <span class="sort-indicator" v-if="sortField === 'update_time'">
                {{ sortOrder === 'asc' ? '↑' : '↓' }}
              </span>
            </th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(item, index) in paginatedData" :key="item.id" 
              :class="{ 'even-row': index % 2 === 1 }">
            <td>{{ item.id }}</td>
            <td>{{ item.name }}</td>
            <td>{{ item.age }}</td>
            <td>
              <span :class="getSexClass(item.sex)">
                {{ getSexText(item.sex) }}
              </span>
            </td>
            <td>{{ item.phone }}</td>
            <td>{{ formatDate(item.entry_time) }}</td>
            <td>{{ item.area_name }}</td>
            <td>{{ formatDateTime(item.create_time) }}</td>
            <td>{{ formatDateTime(item.update_time) }}</td>
            <td class="action-cell">
              <button class="btn btn-info btn-sm" @click="viewTasks(item)">任务</button>
              <div class="dropdown">
                <button class="btn btn-secondary btn-sm dropdown-toggle">更多</button>
                <div class="dropdown-menu">
                  <a href="#" @click="editRepairman(item)">修改</a>
                  <a href="#" @click="viewProfile(item)">详情</a>
                  <a href="#" @click="deleteRepairman(item)" class="text-danger">删除</a>
                </div>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 分页器 -->
    <div class="pagination-container">
      <div class="pagination-info">
        共 {{ totalItems }} 条记录，每页显示 {{ pageSize }} 条
      </div>
      <div class="pagination">
        <button 
          class="btn btn-sm" 
          :disabled="currentPage === 1"
          @click="changePage(currentPage - 1)"
        >
          上一页
        </button>
        <span class="page-numbers">
          <button 
            v-for="page in visiblePages" 
            :key="page"
            class="btn btn-sm"
            :class="{ 'active': page === currentPage }"
            @click="changePage(page)"
          >
            {{ page }}
          </button>
        </span>
        <button 
          class="btn btn-sm"
          :disabled="currentPage === totalPages"
          @click="changePage(currentPage + 1)"
        >
          下一页
        </button>
      </div>
    </div>

    <!-- 添加/编辑检修员对话框 -->
    <div v-if="showAddDialog || showEditDialog" class="modal-overlay" @click="closeDialog">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ showAddDialog ? '添加检修员' : '编辑检修员' }}</h3>
          <button class="close-btn" @click="closeDialog">&times;</button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="saveRepairman">
            <div class="form-group">
              <label>姓名：</label>
              <input 
                v-model="repairmanForm.name" 
                type="text" 
                required 
                class="form-control"
                placeholder="请输入姓名"
                maxlength="5"
              />
            </div>
            <div class="form-group">
              <label>年龄：</label>
              <input 
                v-model="repairmanForm.age" 
                type="number" 
                required 
                class="form-control"
                min="18"
                max="65"
                placeholder="请输入年龄"
              />
            </div>
            <div class="form-group">
              <label>性别：</label>
              <select v-model="repairmanForm.sex" required class="form-control">
                <option value="">请选择性别</option>
                <option value="0">女</option>
                <option value="1">男</option>
              </select>
            </div>
            <div class="form-group">
              <label>联系方式：</label>
              <input 
                v-model="repairmanForm.phone" 
                type="tel" 
                required 
                class="form-control"
                placeholder="请输入11位手机号码"
                pattern="[0-9]{11}"
                maxlength="11"
              />
            </div>
            <div class="form-group">
              <label>入职时间：</label>
              <input 
                v-model="repairmanForm.entry_time" 
                type="date" 
                required 
                class="form-control"
              />
            </div>
            <div class="form-group">
              <label>所属区域：</label>
              <select v-model="repairmanForm.area_id" required class="form-control">
                <option value="">请选择区域</option>
                <option v-for="area in areas" :key="area.id" :value="area.id">
                  {{ area.province }} - {{ area.city }} - {{ area.district }}
                </option>
              </select>
            </div>
            <div class="form-actions">
              <button type="submit" class="btn btn-primary">保存</button>
              <button type="button" class="btn btn-secondary" @click="closeDialog">取消</button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- 检修员详情对话框 -->
    <div v-if="showProfileDialog" class="modal-overlay" @click="closeProfileDialog">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>检修员详情</h3>
          <button class="close-btn" @click="closeProfileDialog">&times;</button>
        </div>
        <div class="modal-body">
          <div class="profile-grid">
            <div class="profile-item">
              <label>检修员ID：</label>
              <span>{{ selectedRepairman.id }}</span>
            </div>
            <div class="profile-item">
              <label>姓名：</label>
              <span>{{ selectedRepairman.name }}</span>
            </div>
            <div class="profile-item">
              <label>年龄：</label>
              <span>{{ selectedRepairman.age }}岁</span>
            </div>
            <div class="profile-item">
              <label>性别：</label>
              <span :class="getSexClass(selectedRepairman.sex)">
                {{ getSexText(selectedRepairman.sex) }}
              </span>
            </div>
            <div class="profile-item">
              <label>联系方式：</label>
              <span>{{ selectedRepairman.phone }}</span>
            </div>
            <div class="profile-item">
              <label>入职时间：</label>
              <span>{{ formatDate(selectedRepairman.entry_time) }}</span>
            </div>
            <div class="profile-item">
              <label>所属区域：</label>
              <span>{{ selectedRepairman.area_name }}</span>
            </div>
            <div class="profile-item">
              <label>工作年限：</label>
              <span>{{ calculateWorkYears(selectedRepairman.entry_time) }}年</span>
            </div>
            <div class="profile-item">
              <label>创建时间：</label>
              <span>{{ formatDateTime(selectedRepairman.create_time) }}</span>
            </div>
            <div class="profile-item">
              <label>更新时间：</label>
              <span>{{ formatDateTime(selectedRepairman.update_time) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 任务列表对话框 -->
    <div v-if="showTasksDialog" class="modal-overlay" @click="closeTasksDialog">
      <div class="modal-content large-modal" @click.stop>
        <div class="modal-header">
          <h3>{{ selectedRepairman.name }} 的任务列表</h3>
          <button class="close-btn" @click="closeTasksDialog">&times;</button>
        </div>
        <div class="modal-body">
          <div class="tasks-table-container">
            <table class="tasks-table">
              <thead>
                <tr>
                  <th>任务ID</th>
                  <th>监测记录ID</th>
                  <th>任务状态</th>
                  <th>发布时间</th>
                  <th>响应时间</th>
                  <th>完成时间</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="task in repairmanTasks" :key="task.id">
                  <td>{{ task.id }}</td>
                  <td>{{ task.inspection_id }}</td>
                  <td>
                    <span :class="getTaskStatusClass(task.result)">
                      {{ getTaskStatusText(task.result) }}
                    </span>
                  </td>
                  <td>{{ formatDateTime(task.public_time) }}</td>
                  <td>{{ formatDateTime(task.response_time) }}</td>
                  <td>{{ formatDateTime(task.accomplish_time) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'RepairmanInfo',
  data() {
    return {
      // 搜索相关
      searchField: '',
      searchKeyword: '',
      sexFilter: '',
      minAge: '',
      maxAge: '',
      
      // 排序相关
      sortField: '',
      sortOrder: 'asc',
      
      // 分页相关
      currentPage: 1,
      pageSize: 20,
      totalItems: 0,
      
      // 对话框相关
      showAddDialog: false,
      showEditDialog: false,
      showProfileDialog: false,
      showTasksDialog: false,
      repairmanForm: {
        id: null,
        name: '',
        age: '',
        sex: '',
        phone: '',
        entry_time: '',
        area_id: ''
      },
      selectedRepairman: {},
      repairmanTasks: [],
      
      // 数据
      repairmanData: [],
      filteredData: [],
      areas: []
    }
  },
  computed: {
    totalPages() {
      return Math.ceil(this.totalItems / this.pageSize)
    },
    paginatedData() {
      const start = (this.currentPage - 1) * this.pageSize
      const end = start + this.pageSize
      return this.filteredData.slice(start, end)
    },
    visiblePages() {
      const pages = []
      const total = this.totalPages
      const current = this.currentPage
      
      if (total <= 7) {
        for (let i = 1; i <= total; i++) {
          pages.push(i)
        }
      } else {
        if (current <= 4) {
          for (let i = 1; i <= 5; i++) {
            pages.push(i)
          }
          pages.push('...')
          pages.push(total)
        } else if (current >= total - 3) {
          pages.push(1)
          pages.push('...')
          for (let i = total - 4; i <= total; i++) {
            pages.push(i)
          }
        } else {
          pages.push(1)
          pages.push('...')
          for (let i = current - 1; i <= current + 1; i++) {
            pages.push(i)
          }
          pages.push('...')
          pages.push(total)
        }
      }
      
      return pages
    }
  },
  mounted() {
    this.loadData()
    this.loadAreas()
  },
  methods: {
    loadData() {
      // 模拟数据加载
      this.repairmanData = [
        {
          id: 1,
          name: '张三',
          age: 28,
          sex: 1,
          phone: '13800138001',
          entry_time: '2020-03-15',
          area_id: 1,
          area_name: '北京市 - 朝阳区 - 望京街道',
          create_time: '2024-01-10 09:00:00',
          update_time: '2024-01-20 14:30:00'
        },
        {
          id: 2,
          name: '李四',
          age: 32,
          sex: 1,
          phone: '13800138002',
          entry_time: '2019-07-20',
          area_id: 2,
          area_name: '上海市 - 浦东新区 - 陆家嘴街道',
          create_time: '2024-01-11 10:15:00',
          update_time: '2024-01-21 16:45:00'
        },
        {
          id: 3,
          name: '王五',
          age: 26,
          sex: 0,
          phone: '13800138003',
          entry_time: '2021-09-10',
          area_id: 3,
          area_name: '广州市 - 天河区 - 珠江新城',
          create_time: '2024-01-12 11:20:00',
          update_time: '2024-01-22 08:30:00'
        },
        {
          id: 4,
          name: '赵六',
          age: 35,
          sex: 1,
          phone: '13800138004',
          entry_time: '2018-05-12',
          area_id: 1,
          area_name: '北京市 - 朝阳区 - 望京街道',
          create_time: '2024-01-13 14:00:00',
          update_time: '2024-01-23 10:15:00'
        }
      ]
      
      this.filteredData = [...this.repairmanData]
      this.totalItems = this.filteredData.length
    },
    
    loadAreas() {
      // 模拟区域数据
      this.areas = [
        { id: 1, province: '北京市', city: '朝阳区', district: '望京街道' },
        { id: 2, province: '上海市', city: '浦东新区', district: '陆家嘴街道' },
        { id: 3, province: '广州市', city: '天河区', district: '珠江新城' }
      ]
    },
    
    handleSearch() {
      let filtered = [...this.repairmanData]
      
      if (this.searchField && this.searchKeyword) {
        filtered = filtered.filter(item => {
          const value = String(item[this.searchField] || '').toLowerCase()
          return value.includes(this.searchKeyword.toLowerCase())
        })
      }
      
      if (this.sexFilter !== '') {
        filtered = filtered.filter(item => item.sex == this.sexFilter)
      }
      
      if (this.minAge) {
        filtered = filtered.filter(item => item.age >= parseInt(this.minAge))
      }
      
      if (this.maxAge) {
        filtered = filtered.filter(item => item.age <= parseInt(this.maxAge))
      }
      
      this.filteredData = filtered
      this.totalItems = filtered.length
      this.currentPage = 1
    },
    
    resetSearch() {
      this.searchField = ''
      this.searchKeyword = ''
      this.sexFilter = ''
      this.minAge = ''
      this.maxAge = ''
      this.filteredData = [...this.repairmanData]
      this.totalItems = this.filteredData.length
      this.currentPage = 1
    },
    
    handleSort(field) {
      if (this.sortField === field) {
        this.sortOrder = this.sortOrder === 'asc' ? 'desc' : 'asc'
      } else {
        this.sortField = field
        this.sortOrder = 'asc'
      }
      
      this.filteredData.sort((a, b) => {
        let aVal = a[field]
        let bVal = b[field]
        
        if (typeof aVal === 'string') {
          aVal = aVal.toLowerCase()
          bVal = bVal.toLowerCase()
        }
        
        if (this.sortOrder === 'asc') {
          return aVal > bVal ? 1 : -1
        } else {
          return aVal < bVal ? 1 : -1
        }
      })
    },
    
    changePage(page) {
      if (page >= 1 && page <= this.totalPages) {
        this.currentPage = page
      }
    },
    
    getSexClass(sex) {
      return sex === 1 ? 'sex-male' : 'sex-female'
    },
    
    getSexText(sex) {
      return sex === 1 ? '男' : '女'
    },
    
    getTaskStatusClass(status) {
      const classes = {
        0: 'status-published',
        1: 'status-accepted',
        2: 'status-completed',
        3: 'status-error'
      }
      return classes[status] || 'status-published'
    },
    
    getTaskStatusText(status) {
      const texts = {
        0: '已发布',
        1: '已接取',
        2: '已完成',
        3: '异常'
      }
      return texts[status] || '未知'
    },
    
    formatDate(date) {
      if (!date) return '-'
      return date
    },
    
    formatDateTime(dateTime) {
      if (!dateTime) return '-'
      return dateTime.replace('T', ' ').substring(0, 19)
    },
    
    calculateWorkYears(entryTime) {
      if (!entryTime) return 0
      const entry = new Date(entryTime)
      const now = new Date()
      const years = (now - entry) / (1000 * 60 * 60 * 24 * 365.25)
      return Math.floor(years)
    },
    
    viewTasks(repairman) {
      this.selectedRepairman = { ...repairman }
      // 模拟加载该检修员的任务数据
      this.repairmanTasks = [
        {
          id: 1,
          inspection_id: 101,
          result: 2,
          public_time: '2024-01-15 09:00:00',
          response_time: '2024-01-15 09:30:00',
          accomplish_time: '2024-01-15 15:00:00'
        },
        {
          id: 2,
          inspection_id: 102,
          result: 1,
          public_time: '2024-01-16 10:00:00',
          response_time: '2024-01-16 10:15:00',
          accomplish_time: null
        }
      ]
      this.showTasksDialog = true
    },
    
    viewProfile(repairman) {
      this.selectedRepairman = { ...repairman }
      this.showProfileDialog = true
    },
    
    editRepairman(repairman) {
      this.repairmanForm = { ...repairman }
      this.showEditDialog = true
    },
    
    deleteRepairman(repairman) {
      if (confirm(`确定要删除检修员 ${repairman.name} 吗？`)) {
        const index = this.repairmanData.findIndex(item => item.id === repairman.id)
        if (index > -1) {
          this.repairmanData.splice(index, 1)
          this.handleSearch()
          alert('删除成功')
        }
      }
    },
    
    saveRepairman() {
      // 验证手机号格式
      if (!/^[0-9]{11}$/.test(this.repairmanForm.phone)) {
        alert('请输入正确的11位手机号码')
        return
      }
      
      if (this.showAddDialog) {
        // 添加新检修员
        const newRepairman = {
          ...this.repairmanForm,
          id: Date.now(),
          area_name: this.areas.find(a => a.id == this.repairmanForm.area_id)?.province + ' - ' + 
                     this.areas.find(a => a.id == this.repairmanForm.area_id)?.city + ' - ' +
                     this.areas.find(a => a.id == this.repairmanForm.area_id)?.district,
          create_time: new Date().toISOString().substring(0, 19).replace('T', ' '),
          update_time: new Date().toISOString().substring(0, 19).replace('T', ' ')
        }
        this.repairmanData.push(newRepairman)
        alert('添加成功')
      } else {
        // 编辑检修员
        const index = this.repairmanData.findIndex(item => item.id === this.repairmanForm.id)
        if (index > -1) {
          this.repairmanData[index] = {
            ...this.repairmanForm,
            area_name: this.areas.find(a => a.id == this.repairmanForm.area_id)?.province + ' - ' + 
                       this.areas.find(a => a.id == this.repairmanForm.area_id)?.city + ' - ' +
                       this.areas.find(a => a.id == this.repairmanForm.area_id)?.district,
            update_time: new Date().toISOString().substring(0, 19).replace('T', ' ')
          }
          alert('修改成功')
        }
      }
      
      this.closeDialog()
      this.handleSearch()
    },
    
    closeDialog() {
      this.showAddDialog = false
      this.showEditDialog = false
      this.repairmanForm = {
        id: null,
        name: '',
        age: '',
        sex: '',
        phone: '',
        entry_time: '',
        area_id: ''
      }
    },
    
    closeProfileDialog() {
      this.showProfileDialog = false
      this.selectedRepairman = {}
    },
    
    closeTasksDialog() {
      this.showTasksDialog = false
      this.selectedRepairman = {}
      this.repairmanTasks = []
    },
    
    exportToExcel() {
      alert('导出Excel功能')
    }
  }
}
</script>

<style scoped>
.repairman-info {
  padding: 20px;
  background: #f8f9fa;
  min-height: 100vh;
}

.module-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 15px 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.module-header h2 {
  margin: 0;
  color: #1E3A8A;
  font-size: 24px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.search-section {
  background: white;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.search-row {
  display: flex;
  align-items: center;
  gap: 15px;
  flex-wrap: wrap;
}

.search-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-item label {
  font-weight: 500;
  color: #333;
  white-space: nowrap;
}

.search-select, .search-input {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.search-select {
  min-width: 150px;
}

.search-input {
  min-width: 150px;
}

.age-input {
  min-width: 80px;
}

.table-container {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  margin-bottom: 20px;
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 1000px;
}

.data-table th {
  background: #1E3A8A;
  color: white;
  padding: 12px;
  text-align: left;
  font-weight: 500;
  border-bottom: 2px solid #0F2C6B;
}

.data-table th.sortable {
  cursor: pointer;
  user-select: none;
  position: relative;
  transition: background-color 0.3s;
}

.data-table th.sortable:hover {
  background: #0F2C6B;
}

.sort-indicator {
  margin-left: 5px;
  font-size: 12px;
}

.data-table td {
  padding: 12px;
  border-bottom: 1px solid #eee;
  transition: background-color 0.3s;
}

.data-table tr:hover td {
  background: #f0f8ff;
}

.data-table tr.even-row td {
  background: #f0f8ff;
}

.data-table tr.even-row:hover td {
  background: #e6f3ff;
}

.action-cell {
  display: flex;
  gap: 8px;
  align-items: center;
}

.dropdown {
  position: relative;
  display: inline-block;
}

.dropdown-toggle {
  cursor: pointer;
}

.dropdown-menu {
  display: none;
  position: absolute;
  top: 100%;
  right: 0;
  background: white;
  border: 1px solid #ddd;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
  z-index: 1000;
  min-width: 100px;
}

.dropdown:hover .dropdown-menu {
  display: block;
}

.dropdown-menu a {
  display: block;
  padding: 8px 12px;
  text-decoration: none;
  color: #333;
  transition: background-color 0.3s;
}

.dropdown-menu a:hover {
  background: #f5f5f5;
}

.dropdown-menu a.text-danger {
  color: #dc3545;
}

.sex-male {
  color: #007bff;
  font-weight: 500;
}

.sex-female {
  color: #e83e8c;
  font-weight: 500;
}

.status-published {
  color: #6c757d;
  font-weight: 500;
}

.status-accepted {
  color: #007bff;
  font-weight: 500;
}

.status-completed {
  color: #28a745;
  font-weight: 500;
}

.status-error {
  color: #dc3545;
  font-weight: 500;
}

.pagination-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  padding: 15px 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.pagination-info {
  color: #666;
  font-size: 14px;
}

.pagination {
  display: flex;
  align-items: center;
  gap: 5px;
}

.page-numbers {
  display: flex;
  gap: 2px;
}

.btn {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: white;
  color: #333;
  cursor: pointer;
  text-decoration: none;
  font-size: 14px;
  transition: all 0.3s;
}

.btn:hover {
  background: #f5f5f5;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn.active {
  background: #1E3A8A;
  color: white;
  border-color: #1E3A8A;
}

.btn-primary {
  background: #1E3A8A;
  color: white;
  border-color: #1E3A8A;
}

.btn-primary:hover {
  background: #0F2C6B;
  border-color: #0F2C6B;
}

.btn-success {
  background: #28a745;
  color: white;
  border-color: #28a745;
}

.btn-success:hover {
  background: #218838;
  border-color: #218838;
}

.btn-info {
  background: #17a2b8;
  color: white;
  border-color: #17a2b8;
}

.btn-info:hover {
  background: #138496;
  border-color: #138496;
}

.btn-secondary {
  background: #6c757d;
  color: white;
  border-color: #6c757d;
}

.btn-secondary:hover {
  background: #5a6268;
  border-color: #5a6268;
}

.btn-sm {
  padding: 4px 8px;
  font-size: 12px;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
}

.large-modal {
  max-width: 800px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  margin: 0;
  color: #1E3A8A;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
}

.close-btn:hover {
  color: #333;
}

.modal-body {
  padding: 20px;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: 500;
  color: #333;
}

.form-control {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.form-control:focus {
  outline: none;
  border-color: #1E3A8A;
  box-shadow: 0 0 0 2px rgba(30, 58, 138, 0.2);
}

.form-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 20px;
}

.profile-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
}

.profile-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.profile-item label {
  font-weight: 500;
  color: #333;
}

.profile-item span {
  color: #666;
}

.tasks-table-container {
  overflow-x: auto;
}

.tasks-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 600px;
}

.tasks-table th {
  background: #f8f9fa;
  padding: 10px;
  text-align: left;
  font-weight: 500;
  border-bottom: 1px solid #ddd;
}

.tasks-table td {
  padding: 10px;
  border-bottom: 1px solid #eee;
}

.tasks-table tr:hover td {
  background: #f8f9fa;
}

.icon-plus::before {
  content: '+';
  margin-right: 5px;
}

.icon-export::before {
  content: '↓';
  margin-right: 5px;
}

@media (max-width: 768px) {
  .search-row {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-item {
    flex-direction: column;
    align-items: stretch;
  }
  
  .header-actions {
    flex-direction: column;
  }
  
  .pagination-container {
    flex-direction: column;
    gap: 10px;
  }
  
  .profile-grid {
    grid-template-columns: 1fr;
  }
  
  .data-table {
    font-size: 12px;
  }
  
  .data-table th,
  .data-table td {
    padding: 8px;
  }
}
</style>