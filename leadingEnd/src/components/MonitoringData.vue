<template>
  <div class="monitoring-data">
    <!-- 页面标题和操作按钮 -->
    <div class="header-section">
      <h2 class="page-title">监测数据管理</h2>
      <div class="header-actions">
        <button class="btn btn-primary" @click="handleAdd">
          <i class="icon-add"></i>
          添加监测数据
        </button>
        <button class="btn btn-success" @click="handleExport">
          <i class="icon-export"></i>
          导出数据
        </button>
      </div>
    </div>

    <!-- 搜索区域 -->
    <div class="search-section">
      <div class="search-form">
        <div class="search-field">
          <label>搜索字段：</label>
          <select v-model="searchFields" multiple class="search-select">
            <option value="id">监测ID</option>
            <option value="pressure">压力值</option>
            <option value="temperature">温度</option>
            <option value="traffic">流量</option>
            <option value="shake">震动值</option>
            <option value="data_status">数据状态</option>
            <option value="sensor_id">传感器ID</option>
            <option value="area">区域</option>
            <option value="pipeline_name">管道名称</option>
          </select>
        </div>
        <div class="search-input">
          <label>搜索内容：</label>
          <input 
            type="text" 
            v-model="searchKeyword" 
            placeholder="请输入搜索内容，支持模糊查询"
            class="search-text"
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="search-actions">
          <button class="btn btn-primary" @click="handleSearch">搜索</button>
          <button class="btn btn-secondary" @click="handleReset">重置</button>
        </div>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-section">
      <div class="table-container">
        <table class="data-table">
          <thead>
            <tr>
              <th @click="handleSort('id')" class="sortable">
                监测ID
                <span class="sort-icon" :class="getSortClass('id')">↕</span>
              </th>
              <th @click="handleSort('pressure')" class="sortable">
                压力值(MPa)
                <span class="sort-icon" :class="getSortClass('pressure')">↕</span>
              </th>
              <th @click="handleSort('temperature')" class="sortable">
                温度(°C)
                <span class="sort-icon" :class="getSortClass('temperature')">↕</span>
              </th>
              <th @click="handleSort('traffic')" class="sortable">
                流量(m³/h)
                <span class="sort-icon" :class="getSortClass('traffic')">↕</span>
              </th>
              <th @click="handleSort('shake')" class="sortable">
                震动值
                <span class="sort-icon" :class="getSortClass('shake')">↕</span>
              </th>
              <th @click="handleSort('data_status')" class="sortable">
                数据状态
                <span class="sort-icon" :class="getSortClass('data_status')">↕</span>
              </th>
              <th>实时图片</th>
              <th @click="handleSort('sensor_id')" class="sortable">
                传感器ID
                <span class="sort-icon" :class="getSortClass('sensor_id')">↕</span>
              </th>
              <th @click="handleSort('area')" class="sortable">
                区域
                <span class="sort-icon" :class="getSortClass('area')">↕</span>
              </th>
              <th @click="handleSort('pipeline_name')" class="sortable">
                所属管道
                <span class="sort-icon" :class="getSortClass('pipeline_name')">↕</span>
              </th>
              <th @click="handleSort('create_time')" class="sortable">
                创建时间
                <span class="sort-icon" :class="getSortClass('create_time')">↕</span>
              </th>
              <th @click="handleSort('update_time')" class="sortable">
                更新时间
                <span class="sort-icon" :class="getSortClass('update_time')">↕</span>
              </th>
              <th class="actions-column">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr 
              v-for="(item, index) in paginatedData" 
              :key="item.id"
              :class="['data-row', { 'even-row': index % 2 === 1 }]"
            >
              <td>{{ item.id }}</td>
              <td>{{ item.pressure }}</td>
              <td>{{ item.temperature }}</td>
              <td>{{ item.traffic }}</td>
              <td>{{ item.shake }}</td>
              <td>
                <span :class="getStatusClass(item.data_status)">
                  {{ getStatusText(item.data_status) }}
                </span>
              </td>
              <td>
                <img 
                  v-if="item.realtime_picture" 
                  :src="item.realtime_picture" 
                  alt="实时图片" 
                  class="realtime-image"
                  @click="previewImage(item.realtime_picture)"
                />
                <span v-else class="no-image">无图片</span>
              </td>
              <td>{{ item.sensor_id }}</td>
              <td>{{ item.area }}</td>
              <td>{{ item.pipeline_name }}</td>
              <td>{{ formatDateTime(item.create_time) }}</td>
              <td>{{ formatDateTime(item.update_time) }}</td>
              <td class="actions-cell">
                <button class="btn btn-info btn-sm" @click="handleViewLog(item)">
                  日志
                </button>
                <div class="dropdown" :class="{ 'active': activeDropdown === item.id }">
                  <button 
                    class="btn btn-secondary btn-sm dropdown-toggle" 
                    @click="toggleDropdown(item.id)"
                  >
                    更多
                  </button>
                  <div class="dropdown-menu" v-show="activeDropdown === item.id">
                    <a href="#" @click.prevent="handleEdit(item)" class="dropdown-item">
                      <i class="icon-edit"></i>修改
                    </a>
                    <a href="#" @click.prevent="handleDelete(item)" class="dropdown-item delete">
                      <i class="icon-delete"></i>删除
                    </a>
                  </div>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页器 -->
      <div class="pagination-section">
        <div class="pagination-info">
          共 {{ totalItems }} 条记录，第 {{ currentPage }} / {{ totalPages }} 页
        </div>
        <div class="pagination-controls">
          <button 
            class="btn btn-sm" 
            :disabled="currentPage === 1"
            @click="goToPage(1)"
          >
            首页
          </button>
          <button 
            class="btn btn-sm" 
            :disabled="currentPage === 1"
            @click="goToPage(currentPage - 1)"
          >
            上一页
          </button>
          <span class="page-numbers">
            <button 
              v-for="page in visiblePages" 
              :key="page"
              class="btn btn-sm page-number"
              :class="{ 'active': page === currentPage }"
              @click="goToPage(page)"
            >
              {{ page }}
            </button>
          </span>
          <button 
            class="btn btn-sm" 
            :disabled="currentPage === totalPages"
            @click="goToPage(currentPage + 1)"
          >
            下一页
          </button>
          <button 
            class="btn btn-sm" 
            :disabled="currentPage === totalPages"
            @click="goToPage(totalPages)"
          >
            末页
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'MonitoringData',
  data() {
    return {
      // 搜索相关
      searchFields: [],
      searchKeyword: '',
      
      // 排序相关
      sortField: '',
      sortDirection: 'asc', // 'asc' 或 'desc'
      
      // 分页相关
      currentPage: 1,
      pageSize: 20,
      
      // 下拉菜单
      activeDropdown: null,
      
      // 模拟数据
      monitoringData: [
        {
          id: 1,
          pressure: 2.45,
          temperature: 25.3,
          traffic: 1250.5,
          shake: 0.02,
          data_status: 0,
          realtime_picture: null,
          sensor_id: 1001,
          area: '北京市朝阳区',
          pipeline_name: '京津管道',
          create_time: '2024-01-15 10:30:25',
          update_time: '2024-01-15 10:30:25'
        },
        {
          id: 2,
          pressure: 2.38,
          temperature: 23.8,
          traffic: 1180.2,
          shake: 0.03,
          data_status: 1,
          realtime_picture: '/images/sensor_002.jpg',
          sensor_id: 1002,
          area: '天津市滨海新区',
          pipeline_name: '京津管道',
          create_time: '2024-01-15 10:25:12',
          update_time: '2024-01-15 10:25:12'
        },
        {
          id: 3,
          pressure: 2.65,
          temperature: 28.1,
          traffic: 1350.8,
          shake: 0.08,
          data_status: 2,
          realtime_picture: '/images/sensor_003.jpg',
          sensor_id: 1003,
          area: '河北省廊坊市',
          pipeline_name: '华北干线',
          create_time: '2024-01-15 10:20:45',
          update_time: '2024-01-15 10:20:45'
        },
        {
          id: 4,
          pressure: 2.85,
          temperature: 32.5,
          traffic: 1420.3,
          shake: 0.15,
          data_status: 3,
          realtime_picture: '/images/sensor_004.jpg',
          sensor_id: 1004,
          area: '山东省济南市',
          pipeline_name: '华北干线',
          create_time: '2024-01-15 10:15:33',
          update_time: '2024-01-15 10:15:33'
        }
      ]
    }
  },
  computed: {
    // 过滤后的数据
    filteredData() {
      if (!this.searchKeyword || this.searchFields.length === 0) {
        return this.monitoringData;
      }
      
      return this.monitoringData.filter(item => {
        return this.searchFields.some(field => {
          const value = String(item[field] || '').toLowerCase();
          return value.includes(this.searchKeyword.toLowerCase());
        });
      });
    },
    
    // 排序后的数据
    sortedData() {
      if (!this.sortField) {
        return this.filteredData;
      }
      
      return [...this.filteredData].sort((a, b) => {
        let aVal = a[this.sortField];
        let bVal = b[this.sortField];
        
        // 处理数字类型
        if (typeof aVal === 'number' && typeof bVal === 'number') {
          return this.sortDirection === 'asc' ? aVal - bVal : bVal - aVal;
        }
        
        // 处理字符串类型
        aVal = String(aVal || '').toLowerCase();
        bVal = String(bVal || '').toLowerCase();
        
        if (this.sortDirection === 'asc') {
          return aVal.localeCompare(bVal);
        } else {
          return bVal.localeCompare(aVal);
        }
      });
    },
    
    // 分页后的数据
    paginatedData() {
      const start = (this.currentPage - 1) * this.pageSize;
      const end = start + this.pageSize;
      return this.sortedData.slice(start, end);
    },
    
    // 总条数
    totalItems() {
      return this.sortedData.length;
    },
    
    // 总页数
    totalPages() {
      return Math.ceil(this.totalItems / this.pageSize);
    },
    
    // 可见的页码
    visiblePages() {
      const pages = [];
      const start = Math.max(1, this.currentPage - 2);
      const end = Math.min(this.totalPages, this.currentPage + 2);
      
      for (let i = start; i <= end; i++) {
        pages.push(i);
      }
      
      return pages;
    }
  },
  methods: {
    // 搜索功能
    handleSearch() {
      this.currentPage = 1; // 重置到第一页
      // 这里可以添加API调用逻辑
      console.log('搜索条件:', {
        fields: this.searchFields,
        keyword: this.searchKeyword
      });
    },
    
    // 重置搜索
    handleReset() {
      this.searchFields = [];
      this.searchKeyword = '';
      this.currentPage = 1;
    },
    
    // 排序功能
    handleSort(field) {
      if (this.sortField === field) {
        // 切换排序方向
        this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
      } else {
        // 新字段，默认升序
        this.sortField = field;
        this.sortDirection = 'asc';
      }
      this.currentPage = 1; // 重置到第一页
    },
    
    // 获取排序图标样式
    getSortClass(field) {
      if (this.sortField !== field) return '';
      return this.sortDirection === 'asc' ? 'sort-asc' : 'sort-desc';
    },
    
    // 分页功能
    goToPage(page) {
      if (page >= 1 && page <= this.totalPages) {
        this.currentPage = page;
      }
    },
    
    // 获取状态样式
    getStatusClass(status) {
      const statusMap = {
        0: 'status-safe',
        1: 'status-good',
        2: 'status-danger',
        3: 'status-critical'
      };
      return statusMap[status] || '';
    },
    
    // 获取状态文本
    getStatusText(status) {
      const statusMap = {
        0: '安全',
        1: '良好',
        2: '危险',
        3: '高危'
      };
      return statusMap[status] || '未知';
    },
    
    // 格式化日期时间
    formatDateTime(dateTime) {
      if (!dateTime) return '';
      return new Date(dateTime).toLocaleString('zh-CN');
    },
    
    // 预览图片
    previewImage(imageSrc) {
      // 这里可以实现图片预览功能
      console.log('预览图片:', imageSrc);
    },
    
    // 下拉菜单控制
    toggleDropdown(id) {
      this.activeDropdown = this.activeDropdown === id ? null : id;
    },
    
    // 功能按钮事件
    handleAdd() {
      console.log('添加监测数据');
      // 这里可以打开添加对话框或跳转到添加页面
    },
    
    handleExport() {
      console.log('导出数据');
      // 这里可以实现导出Excel功能
    },
    
    handleViewLog(item) {
      console.log('查看日志:', item);
      // 这里可以打开日志查看页面
    },
    
    handleEdit(item) {
      console.log('编辑:', item);
      this.activeDropdown = null;
      // 这里可以打开编辑对话框
    },
    
    handleDelete(item) {
      console.log('删除:', item);
      this.activeDropdown = null;
      // 这里可以显示确认删除对话框
    }
  },
  
  // 点击外部关闭下拉菜单
  mounted() {
    document.addEventListener('click', (e) => {
      if (!e.target.closest('.dropdown')) {
        this.activeDropdown = null;
      }
    });
  }
}
</script>

<style scoped>
.monitoring-data {
  padding: 20px;
  background: #f5f5f5;
  min-height: 100vh;
}

/* 页面标题区域 */
.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.page-title {
  color: #1E3A8A;
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 10px;
}

/* 搜索区域 */
.search-section {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
}

.search-form {
  display: flex;
  gap: 20px;
  align-items: end;
  flex-wrap: wrap;
}

.search-field, .search-input {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.search-field label, .search-input label {
  font-weight: 500;
  color: #333;
}

.search-select {
  min-width: 200px;
  height: 36px;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.search-text {
  min-width: 300px;
  height: 36px;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.search-actions {
  display: flex;
  gap: 10px;
}

/* 表格区域 */
.table-section {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.table-container {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.data-table th {
  background: #1E3A8A;
  color: white;
  padding: 12px 8px;
  text-align: left;
  font-weight: 500;
  white-space: nowrap;
  position: relative;
}

.data-table th.sortable {
  cursor: pointer;
  user-select: none;
  transition: background-color 0.3s ease;
}

.data-table th.sortable:hover {
  background: #0F2C6B;
}

.sort-icon {
  margin-left: 5px;
  font-size: 12px;
  opacity: 0.6;
  transition: all 0.3s ease;
}

.sort-icon.sort-asc {
  opacity: 1;
  transform: rotate(180deg);
}

.sort-icon.sort-desc {
  opacity: 1;
  transform: rotate(0deg);
}

.data-table td {
  padding: 12px 8px;
  border-bottom: 1px solid #eee;
  white-space: nowrap;
}

/* 数据行样式 */
.data-row {
  background: white;
  transition: background-color 0.3s ease;
}

.data-row:hover {
  background: #f0f7ff !important;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(30, 58, 138, 0.1);
}

.data-row.even-row {
  background: #f8faff;
}

/* 状态标签 */
.status-safe {
  background: #d1fae5;
  color: #059669;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-good {
  background: #dbeafe;
  color: #2563eb;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-danger {
  background: #fef3c7;
  color: #d97706;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-critical {
  background: #fee2e2;
  color: #dc2626;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

/* 实时图片 */
.realtime-image {
  width: 40px;
  height: 30px;
  object-fit: cover;
  border-radius: 4px;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.realtime-image:hover {
  transform: scale(1.1);
}

.no-image {
  color: #999;
  font-size: 12px;
}

/* 操作列 */
.actions-column {
  width: 120px;
}

.actions-cell {
  display: flex;
  gap: 5px;
  align-items: center;
}

/* 下拉菜单 */
.dropdown {
  position: relative;
  display: inline-block;
}

.dropdown-toggle {
  position: relative;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  background: white;
  border: 1px solid #ddd;
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  min-width: 100px;
}

.dropdown-item {
  display: block;
  padding: 8px 12px;
  color: #333;
  text-decoration: none;
  font-size: 13px;
  transition: background-color 0.3s ease;
}

.dropdown-item:hover {
  background: #f5f5f5;
}

.dropdown-item.delete {
  color: #dc2626;
}

.dropdown-item.delete:hover {
  background: #fee2e2;
}

/* 分页区域 */
.pagination-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-top: 1px solid #eee;
}

.pagination-info {
  color: #666;
  font-size: 14px;
}

.pagination-controls {
  display: flex;
  gap: 5px;
  align-items: center;
}

.page-numbers {
  display: flex;
  gap: 2px;
}

.page-number.active {
  background: #1E3A8A;
  color: white;
}

/* 按钮样式 */
.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  background: #1E3A8A;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #0F2C6B;
}

.btn-secondary {
  background: #6b7280;
  color: white;
}

.btn-secondary:hover:not(:disabled) {
  background: #4b5563;
}

.btn-success {
  background: #059669;
  color: white;
}

.btn-success:hover:not(:disabled) {
  background: #047857;
}

.btn-info {
  background: #0891b2;
  color: white;
}

.btn-info:hover:not(:disabled) {
  background: #0e7490;
}

.btn-sm {
  padding: 4px 8px;
  font-size: 12px;
}

/* 图标样式 */
.icon-add::before { content: '+'; }
.icon-export::before { content: '↓'; }
.icon-edit::before { content: '✎'; }
.icon-delete::before { content: '×'; }

/* 响应式设计 */
@media (max-width: 768px) {
  .header-section {
    flex-direction: column;
    gap: 15px;
    align-items: stretch;
  }
  
  .search-form {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-select, .search-text {
    min-width: auto;
    width: 100%;
  }
  
  .pagination-section {
    flex-direction: column;
    gap: 15px;
  }
  
  .table-container {
    font-size: 12px;
  }
  
  .data-table th, .data-table td {
    padding: 8px 4px;
  }
}
</style>