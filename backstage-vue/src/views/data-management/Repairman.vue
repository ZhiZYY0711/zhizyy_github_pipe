<template>
  <div class="repairman-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2 class="page-title">检修员管理</h2>
      <div class="header-actions">
        <button class="btn btn-primary" @click="showAddModal = true">
          <i class="icon">+</i> 添加检修员
        </button>
        <button class="btn btn-secondary" @click="refreshData">
          <i class="icon">↻</i> 刷新
        </button>
      </div>
    </div>

    <!-- 检修员统计卡片 -->
    <div class="stats-section">
      <div class="stat-card">
        <div class="stat-number">{{ repairmanStats.total }}</div>
        <div class="stat-label">总检修员数</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ repairmanStats.male }}</div>
        <div class="stat-label">男性</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ repairmanStats.female }}</div>
        <div class="stat-label">女性</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ repairmanStats.avgAge }}</div>
        <div class="stat-label">平均年龄</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ repairmanStats.newThisMonth }}</div>
        <div class="stat-label">本月新增</div>
      </div>
    </div>

    <!-- 搜索和筛选区域 -->
    <div class="filter-section">
      <div class="filter-row">
        <div class="filter-item">
          <label>姓名：</label>
          <input v-model="searchForm.name" placeholder="请输入检修员姓名" class="input-field">
        </div>
        <div class="filter-item">
          <label>性别：</label>
          <select v-model="searchForm.sex" class="select-field">
            <option value="">全部性别</option>
            <option value="0">男</option>
            <option value="1">女</option>
          </select>
        </div>
        <div class="filter-item">
          <label>年龄范围：</label>
          <div class="age-range">
            <input v-model="searchForm.minAge" placeholder="最小" class="input-field age-input" type="number">
            <span>-</span>
            <input v-model="searchForm.maxAge" placeholder="最大" class="input-field age-input" type="number">
          </div>
        </div>
        <div class="filter-item">
          <label>所属区域：</label>
          <select v-model="searchForm.areaId" class="select-field">
            <option value="">全部区域</option>
            <option v-for="area in areas" :key="area.id" :value="area.id">
              {{ area.province }} {{ area.city }} {{ area.district }}
            </option>
          </select>
        </div>
        <button class="btn btn-search" @click="searchRepairman">搜索</button>
      </div>
    </div>

    <!-- 检修员列表 -->
    <div class="repairman-list">
      <div class="table-container">
        <table class="data-table">
          <thead>
            <tr>
              <th class="col-id">员工编号</th>
              <th class="col-name">姓名</th>
              <th class="col-age">年龄</th>
              <th class="col-sex">性别</th>
              <th class="col-phone">联系方式</th>
              <th class="col-area">所属区域</th>
              <th class="col-entry-time">入职时间</th>
              <th class="col-actions">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="repairman in repairmanList" :key="repairman.id" class="table-row">
              <td class="col-id">
                <div class="cell-content">
                  <span class="employee-code">RM{{ String(repairman.id).padStart(4, '0') }}</span>
                </div>
              </td>
              <td class="col-name">
                <div class="cell-content">
                  <div class="repairman-info">
                    <span class="name">{{ repairman.name }}</span>
                  </div>
                </div>
              </td>
              <td class="col-age">
                <div class="cell-content">
                  <span class="age">{{ repairman.age }}岁</span>
                </div>
              </td>
              <td class="col-sex">
                <div class="cell-content">
                  <span class="sex" :class="repairman.sex === 0 ? 'male' : 'female'">
                    {{ repairman.sex === 0 ? '男' : '女' }}
                  </span>
                </div>
              </td>
              <td class="col-phone">
                <div class="cell-content">
                  <span class="phone">{{ repairman.phone }}</span>
                </div>
              </td>
              <td class="col-area">
                <div class="cell-content">
                  <div class="area-info">
                    <span class="area">{{ getAreaName(repairman.area_id) }}</span>
                  </div>
                </div>
              </td>
              <td class="col-entry-time">
                <div class="cell-content">
                  <span class="time">{{ formatDate(repairman.entry_time) }}</span>
                </div>
              </td>
              <td class="col-actions">
                <div class="cell-content">
                  <div class="action-buttons">
                    <button class="action-btn view-btn" @click="viewRepairman(repairman)" title="查看详情">
                      <i class="icon">👁</i>
                    </button>
                    <button class="action-btn edit-btn" @click="editRepairman(repairman)" title="编辑">
                      <i class="icon">✏</i>
                    </button>
                    <button class="action-btn delete-btn" @click="deleteRepairman(repairman)" title="删除">
                      <i class="icon">🗑</i>
                    </button>
                  </div>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div class="pagination">
        <button class="page-btn" @click="prevPage" :disabled="currentPage === 1">上一页</button>
        <span class="page-info">第 {{ currentPage }} 页，共 {{ totalPages }} 页</span>
        <button class="page-btn" @click="nextPage" :disabled="currentPage === totalPages">下一页</button>
      </div>
    </div>

    <!-- 添加/编辑检修员模态框 -->
    <div v-if="showAddModal || showEditModal" class="modal-overlay" @click="closeModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ showAddModal ? '添加检修员' : '编辑检修员' }}</h3>
          <button class="close-btn" @click="closeModal">&times;</button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="saveRepairman">
            <div class="form-row">
              <div class="form-group">
                <label>姓名 *</label>
                <input v-model="formData.name" class="form-input" placeholder="请输入姓名" required maxlength="5">
              </div>
              <div class="form-group">
                <label>年龄 *</label>
                <input v-model="formData.age" class="form-input" type="number" placeholder="请输入年龄" required min="18" max="65">
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>性别 *</label>
                <select v-model="formData.sex" class="form-input" required>
                  <option value="">请选择性别</option>
                  <option value="0">男</option>
                  <option value="1">女</option>
                </select>
              </div>
              <div class="form-group">
                <label>联系方式 *</label>
                <input v-model="formData.phone" class="form-input" placeholder="请输入手机号码" required pattern="^1[3-9]\d{9}$">
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>所属区域 *</label>
                <select v-model="formData.area_id" class="form-input" required>
                  <option value="">请选择区域</option>
                  <option v-for="area in areas" :key="area.id" :value="area.id">
                    {{ area.province }} {{ area.city }} {{ area.district }}
                  </option>
                </select>
              </div>
              <div class="form-group">
                <label>入职时间 *</label>
                <input v-model="formData.entry_time" class="form-input" type="date" required>
              </div>
            </div>
            <div class="form-actions">
              <button type="button" class="btn btn-secondary" @click="closeModal">取消</button>
              <button type="submit" class="btn btn-primary">{{ showAddModal ? '添加' : '保存' }}</button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- 查看检修员详情模态框 -->
    <div v-if="showDetailModal" class="modal-overlay" @click="closeModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>检修员详情</h3>
          <button class="close-btn" @click="closeModal">&times;</button>
        </div>
        <div class="modal-body">
          <div class="detail-grid">
            <div class="detail-item">
              <label>员工编号</label>
              <span>RM{{ String(selectedRepairman.id).padStart(4, '0') }}</span>
            </div>
            <div class="detail-item">
              <label>姓名</label>
              <span>{{ selectedRepairman.name }}</span>
            </div>
            <div class="detail-item">
              <label>年龄</label>
              <span>{{ selectedRepairman.age }}岁</span>
            </div>
            <div class="detail-item">
              <label>性别</label>
              <span>{{ selectedRepairman.sex === 0 ? '男' : '女' }}</span>
            </div>
            <div class="detail-item">
              <label>联系方式</label>
              <span>{{ selectedRepairman.phone }}</span>
            </div>
            <div class="detail-item">
              <label>所属区域</label>
              <span>{{ getAreaName(selectedRepairman.area_id) }}</span>
            </div>
            <div class="detail-item">
              <label>入职时间</label>
              <span>{{ formatDate(selectedRepairman.entry_time) }}</span>
            </div>
            <div class="detail-item">
              <label>工作年限</label>
              <span>{{ calculateWorkYears(selectedRepairman.entry_time) }}年</span>
            </div>
            <div class="detail-item">
              <label>创建时间</label>
              <span>{{ formatDateTime(selectedRepairman.create_time) }}</span>
            </div>
            <div class="detail-item">
              <label>更新时间</label>
              <span>{{ formatDateTime(selectedRepairman.update_time) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Repairman',
  data() {
    return {
      // 检修员列表
      repairmanList: [
        {
          id: 1,
          name: '张维护',
          age: 32,
          sex: 0,
          phone: '13812345678',
          entry_time: '2020-03-15',
          area_id: 1,
          create_time: '2020-03-15 09:00:00',
          update_time: '2024-01-20 14:30:00'
        },
        {
          id: 2,
          name: '李检修',
          age: 28,
          sex: 1,
          phone: '13987654321',
          entry_time: '2021-06-20',
          area_id: 2,
          create_time: '2021-06-20 10:00:00',
          update_time: '2024-01-18 16:45:00'
        },
        {
          id: 3,
          name: '王师傅',
          age: 45,
          sex: 0,
          phone: '13765432109',
          entry_time: '2018-01-10',
          area_id: 1,
          create_time: '2018-01-10 08:30:00',
          update_time: '2024-01-15 11:20:00'
        },
        {
          id: 4,
          name: '赵技术',
          age: 35,
          sex: 0,
          phone: '13654321098',
          entry_time: '2019-09-25',
          area_id: 3,
          create_time: '2019-09-25 14:15:00',
          update_time: '2024-01-22 09:10:00'
        }
      ],
      
      // 区域列表
      areas: [
        { id: 1, province: '北京市', city: '北京市', district: '朝阳区' },
        { id: 2, province: '上海市', city: '上海市', district: '浦东新区' },
        { id: 3, province: '广东省', city: '深圳市', district: '南山区' },
        { id: 4, province: '江苏省', city: '南京市', district: '鼓楼区' }
      ],

      // 搜索表单
      searchForm: {
        name: '',
        sex: '',
        minAge: '',
        maxAge: '',
        areaId: ''
      },

      // 表单数据
      formData: {
        name: '',
        age: '',
        sex: '',
        phone: '',
        area_id: '',
        entry_time: ''
      },

      // 模态框状态
      showAddModal: false,
      showEditModal: false,
      showDetailModal: false,
      selectedRepairman: {},

      // 分页
      currentPage: 1,
      pageSize: 10,
      totalItems: 0
    }
  },

  computed: {
    // 检修员统计
    repairmanStats() {
      const total = this.repairmanList.length;
      const male = this.repairmanList.filter(r => r.sex === 0).length;
      const female = this.repairmanList.filter(r => r.sex === 1).length;
      const avgAge = total > 0 ? Math.round(this.repairmanList.reduce((sum, r) => sum + r.age, 0) / total) : 0;
      
      // 计算本月新增（简化处理）
      const thisMonth = new Date().getMonth() + 1;
      const newThisMonth = this.repairmanList.filter(r => {
        const entryMonth = new Date(r.entry_time).getMonth() + 1;
        return entryMonth === thisMonth;
      }).length;

      return {
        total,
        male,
        female,
        avgAge,
        newThisMonth
      };
    },

    // 总页数
    totalPages() {
      return Math.ceil(this.totalItems / this.pageSize);
    }
  },

  mounted() {
    this.loadRepairmanList();
  },

  methods: {
    // 加载检修员列表
    loadRepairmanList() {
      // 模拟API调用
      this.totalItems = this.repairmanList.length;
    },

    // 搜索检修员
    searchRepairman() {
      // 模拟搜索逻辑
      console.log('搜索检修员:', this.searchForm);
      this.loadRepairmanList();
    },

    // 刷新数据
    refreshData() {
      this.loadRepairmanList();
      this.$message?.success('数据已刷新');
    },

    // 查看检修员详情
    viewRepairman(repairman) {
      this.selectedRepairman = { ...repairman };
      this.showDetailModal = true;
    },

    // 编辑检修员
    editRepairman(repairman) {
      this.formData = { ...repairman };
      this.showEditModal = true;
    },

    // 删除检修员
    deleteRepairman(repairman) {
      if (confirm(`确定要删除检修员 ${repairman.name} 吗？`)) {
        const index = this.repairmanList.findIndex(r => r.id === repairman.id);
        if (index > -1) {
          this.repairmanList.splice(index, 1);
          this.$message?.success('删除成功');
          this.loadRepairmanList();
        }
      }
    },

    // 保存检修员
    saveRepairman() {
      if (this.showAddModal) {
        // 添加新检修员
        const newId = Math.max(...this.repairmanList.map(r => r.id)) + 1;
        const newRepairman = {
          ...this.formData,
          id: newId,
          age: parseInt(this.formData.age),
          sex: parseInt(this.formData.sex),
          area_id: parseInt(this.formData.area_id),
          create_time: new Date().toISOString().slice(0, 19).replace('T', ' '),
          update_time: new Date().toISOString().slice(0, 19).replace('T', ' ')
        };
        this.repairmanList.push(newRepairman);
        this.$message?.success('添加成功');
      } else {
        // 编辑检修员
        const index = this.repairmanList.findIndex(r => r.id === this.formData.id);
        if (index > -1) {
          this.repairmanList[index] = {
            ...this.formData,
            age: parseInt(this.formData.age),
            sex: parseInt(this.formData.sex),
            area_id: parseInt(this.formData.area_id),
            update_time: new Date().toISOString().slice(0, 19).replace('T', ' ')
          };
          this.$message?.success('保存成功');
        }
      }
      this.closeModal();
      this.loadRepairmanList();
    },

    // 关闭模态框
    closeModal() {
      this.showAddModal = false;
      this.showEditModal = false;
      this.showDetailModal = false;
      this.formData = {
        name: '',
        age: '',
        sex: '',
        phone: '',
        area_id: '',
        entry_time: ''
      };
    },

    // 获取区域名称
    getAreaName(areaId) {
      const area = this.areas.find(a => a.id === areaId);
      return area ? `${area.province} ${area.city} ${area.district}` : '未知区域';
    },

    // 格式化日期
    formatDate(dateStr) {
      if (!dateStr) return '-';
      return new Date(dateStr).toLocaleDateString('zh-CN');
    },

    // 格式化日期时间
    formatDateTime(dateStr) {
      if (!dateStr) return '-';
      return new Date(dateStr).toLocaleString('zh-CN');
    },

    // 计算工作年限
    calculateWorkYears(entryTime) {
      if (!entryTime) return 0;
      const entry = new Date(entryTime);
      const now = new Date();
      const years = (now - entry) / (1000 * 60 * 60 * 24 * 365);
      return Math.floor(years);
    },

    // 分页
    prevPage() {
      if (this.currentPage > 1) {
        this.currentPage--;
        this.loadRepairmanList();
      }
    },

    nextPage() {
      if (this.currentPage < this.totalPages) {
        this.currentPage++;
        this.loadRepairmanList();
      }
    }
  }
}
</script>

<style scoped>
.repairman-container {
  padding: 24px;
  background-color: #f8fafc;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 20px 24px;
  background: linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%);
  border-radius: 12px;
  color: white;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.page-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-primary {
  background: linear-gradient(135deg, #059669 0%, #10b981 100%);
  color: white;
  box-shadow: 0 2px 4px rgba(16, 185, 129, 0.2);
}

.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(16, 185, 129, 0.3);
}

.btn-secondary {
  background: rgba(255, 255, 255, 0.2);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.btn-secondary:hover {
  background: rgba(255, 255, 255, 0.3);
}

.btn-search {
  background: linear-gradient(135deg, #1e40af 0%, #3b82f6 100%);
  color: white;
  box-shadow: 0 2px 4px rgba(59, 130, 246, 0.2);
}

.btn-search:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(59, 130, 246, 0.3);
}

.stats-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  padding: 24px;
  border-radius: 12px;
  text-align: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  border: 1px solid #e2e8f0;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
}

.stat-number {
  font-size: 32px;
  font-weight: 700;
  color: #1e40af;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #64748b;
  font-weight: 500;
}

.filter-section {
  background: white;
  padding: 24px;
  border-radius: 12px;
  margin-bottom: 24px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  border: 1px solid #e2e8f0;
}

.filter-row {
  display: flex;
  gap: 20px;
  align-items: end;
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 150px;
}

.filter-item label {
  font-weight: 500;
  color: #374151;
  font-size: 14px;
}

.input-field, .select-field {
  padding: 10px 12px;
  border: 2px solid #e2e8f0;
  border-radius: 6px;
  font-size: 14px;
  transition: border-color 0.2s ease;
}

.input-field:focus, .select-field:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.age-range {
  display: flex;
  align-items: center;
  gap: 8px;
}

.age-input {
  width: 80px;
}

.repairman-list {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  border: 1px solid #e2e8f0;
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
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
  padding: 16px 12px;
  text-align: left;
  font-weight: 600;
  color: #374151;
  border-bottom: 2px solid #e2e8f0;
  white-space: nowrap;
}

.data-table td {
  padding: 16px 12px;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: middle;
}

.table-row:hover {
  background-color: #f8fafc;
}

.col-id { width: 120px; }
.col-name { width: 100px; }
.col-age { width: 80px; }
.col-sex { width: 60px; }
.col-phone { width: 130px; }
.col-area { width: 180px; }
.col-entry-time { width: 120px; }
.col-actions { width: 120px; }

.cell-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.employee-code {
  font-family: 'Courier New', monospace;
  font-weight: 600;
  color: #1e40af;
  background: #dbeafe;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.repairman-info .name {
  font-weight: 500;
  color: #374151;
}

.age {
  color: #64748b;
}

.sex {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.sex.male {
  background: #dbeafe;
  color: #1e40af;
}

.sex.female {
  background: #fce7f3;
  color: #be185d;
}

.phone {
  font-family: 'Courier New', monospace;
  color: #374151;
}

.area-info .area {
  color: #64748b;
  font-size: 13px;
}

.time {
  color: #64748b;
  font-size: 13px;
}

.action-buttons {
  display: flex;
  gap: 4px;
}

.action-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  transition: all 0.2s ease;
  position: relative;
}

.view-btn {
  background: linear-gradient(135deg, #059669 0%, #10b981 100%);
  color: white;
  box-shadow: 0 2px 4px rgba(16, 185, 129, 0.2);
}

.edit-btn {
  background: linear-gradient(135deg, #d97706 0%, #f59e0b 100%);
  color: white;
  box-shadow: 0 2px 4px rgba(245, 158, 11, 0.2);
}

.delete-btn {
  background: linear-gradient(135deg, #dc2626 0%, #ef4444 100%);
  color: white;
  box-shadow: 0 2px 4px rgba(239, 68, 68, 0.2);
}

.action-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.action-btn:active {
  transform: translateY(0);
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
}

.page-btn {
  padding: 8px 16px;
  border: 2px solid #e2e8f0;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  transition: all 0.2s ease;
}

.page-btn:hover:not(:disabled) {
  background: #f1f5f9;
  border-color: #3b82f6;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 800px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
}

.modal-header {
  padding: 24px;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%);
  color: white;
  border-radius: 12px 12px 0 0;
}

.modal-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: white;
  opacity: 0.8;
  transition: opacity 0.2s ease;
}

.close-btn:hover {
  opacity: 1;
}

.modal-body {
  padding: 24px;
}

.form-row {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.form-group {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-weight: 500;
  color: #374151;
  font-size: 14px;
}

.form-input {
  padding: 12px 16px;
  border: 2px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.2s ease;
}

.form-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #f1f5f9;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-item label {
  font-weight: 600;
  color: #374151;
  font-size: 14px;
}

.detail-item span {
  color: #64748b;
  font-size: 14px;
}

.icon {
  font-style: normal;
}

@media (max-width: 768px) {
  .filter-row {
    flex-direction: column;
    align-items: stretch;
  }
  
  .form-row {
    flex-direction: column;
  }
  
  .header-actions {
    flex-direction: column;
  }
  
  .stats-section {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .detail-grid {
    grid-template-columns: 1fr;
  }
  
  .age-range {
    flex-direction: column;
    align-items: stretch;
  }
  
  .age-input {
    width: 100%;
  }
}
</style>