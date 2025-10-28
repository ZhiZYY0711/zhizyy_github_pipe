import { 
  getEquipmentList, 
  createEquipment, 
  updateEquipment, 
  deleteEquipment,
  getTaskList,
  createTask,
  updateTask,
  deleteTask,
  getTaskDetails,
  getRepairmanList,
  createRepairman,
  updateRepairman,
  deleteRepairman
} from '../api'

const state = {
  // 设备管理
  equipment: {
    list: [],
    total: 0,
    currentPage: 1,
    pageSize: 10
  },
  // 任务管理
  tasks: {
    list: [],
    total: 0,
    currentPage: 1,
    pageSize: 10,
    currentTask: null
  },
  // 维修人员管理
  repairmen: {
    list: [],
    total: 0,
    currentPage: 1,
    pageSize: 10
  },
  // 加载状态
  loading: {
    equipment: false,
    tasks: false,
    repairmen: false,
    taskDetails: false
  },
  // 表单状态
  forms: {
    equipmentForm: {},
    taskForm: {},
    repairmanForm: {}
  }
}

const mutations = {
  // 设备管理
  SET_EQUIPMENT_LIST(state, { list, total }) {
    state.equipment.list = list
    state.equipment.total = total
  },
  SET_EQUIPMENT_PAGE(state, { currentPage, pageSize }) {
    state.equipment.currentPage = currentPage
    if (pageSize) state.equipment.pageSize = pageSize
  },
  ADD_EQUIPMENT(state, equipment) {
    state.equipment.list.unshift(equipment)
    state.equipment.total += 1
  },
  UPDATE_EQUIPMENT(state, updatedEquipment) {
    const index = state.equipment.list.findIndex(item => item.id === updatedEquipment.id)
    if (index !== -1) {
      state.equipment.list.splice(index, 1, updatedEquipment)
    }
  },
  REMOVE_EQUIPMENT(state, equipmentId) {
    const index = state.equipment.list.findIndex(item => item.id === equipmentId)
    if (index !== -1) {
      state.equipment.list.splice(index, 1)
      state.equipment.total -= 1
    }
  },

  // 任务管理
  SET_TASK_LIST(state, { list, total }) {
    state.tasks.list = list
    state.tasks.total = total
  },
  SET_TASK_PAGE(state, { currentPage, pageSize }) {
    state.tasks.currentPage = currentPage
    if (pageSize) state.tasks.pageSize = pageSize
  },
  SET_CURRENT_TASK(state, task) {
    state.tasks.currentTask = task
  },
  ADD_TASK(state, task) {
    state.tasks.list.unshift(task)
    state.tasks.total += 1
  },
  UPDATE_TASK(state, updatedTask) {
    const index = state.tasks.list.findIndex(item => item.id === updatedTask.id)
    if (index !== -1) {
      state.tasks.list.splice(index, 1, updatedTask)
    }
    if (state.tasks.currentTask && state.tasks.currentTask.id === updatedTask.id) {
      state.tasks.currentTask = updatedTask
    }
  },
  REMOVE_TASK(state, taskId) {
    const index = state.tasks.list.findIndex(item => item.id === taskId)
    if (index !== -1) {
      state.tasks.list.splice(index, 1)
      state.tasks.total -= 1
    }
  },

  // 维修人员管理
  SET_REPAIRMAN_LIST(state, { list, total }) {
    state.repairmen.list = list
    state.repairmen.total = total
  },
  SET_REPAIRMAN_PAGE(state, { currentPage, pageSize }) {
    state.repairmen.currentPage = currentPage
    if (pageSize) state.repairmen.pageSize = pageSize
  },
  ADD_REPAIRMAN(state, repairman) {
    state.repairmen.list.unshift(repairman)
    state.repairmen.total += 1
  },
  UPDATE_REPAIRMAN(state, updatedRepairman) {
    const index = state.repairmen.list.findIndex(item => item.id === updatedRepairman.id)
    if (index !== -1) {
      state.repairmen.list.splice(index, 1, updatedRepairman)
    }
  },
  REMOVE_REPAIRMAN(state, repairmanId) {
    const index = state.repairmen.list.findIndex(item => item.id === repairmanId)
    if (index !== -1) {
      state.repairmen.list.splice(index, 1)
      state.repairmen.total -= 1
    }
  },

  // 加载状态
  SET_LOADING(state, { type, loading }) {
    state.loading[type] = loading
  },

  // 表单状态
  SET_FORM_DATA(state, { formType, data }) {
    state.forms[formType] = { ...state.forms[formType], ...data }
  },
  CLEAR_FORM_DATA(state, formType) {
    state.forms[formType] = {}
  }
}

const actions = {
  // 设备管理操作
  async fetchEquipmentList({ commit, state }, params = {}) {
    commit('SET_LOADING', { type: 'equipment', loading: true })
    try {
      const queryParams = {
        page: state.equipment.currentPage,
        pageSize: state.equipment.pageSize,
        ...params
      }
      const data = await getEquipmentList(queryParams)
      commit('SET_EQUIPMENT_LIST', data)
      return data
    } catch (error) {
      console.error('获取设备列表失败:', error)
      throw error
    } finally {
      commit('SET_LOADING', { type: 'equipment', loading: false })
    }
  },

  async addEquipment({ commit }, equipmentData) {
    try {
      const equipment = await createEquipment(equipmentData)
      commit('ADD_EQUIPMENT', equipment)
      return equipment
    } catch (error) {
      console.error('添加设备失败:', error)
      throw error
    }
  },

  async updateEquipment({ commit }, { id, data }) {
    try {
      const equipment = await updateEquipment(id, data)
      commit('UPDATE_EQUIPMENT', equipment)
      return equipment
    } catch (error) {
      console.error('更新设备失败:', error)
      throw error
    }
  },

  async removeEquipment({ commit }, equipmentId) {
    try {
      await deleteEquipment(equipmentId)
      commit('REMOVE_EQUIPMENT', equipmentId)
    } catch (error) {
      console.error('删除设备失败:', error)
      throw error
    }
  },

  // 任务管理操作
  async fetchTaskList({ commit, state }, params = {}) {
    commit('SET_LOADING', { type: 'tasks', loading: true })
    try {
      const queryParams = {
        page: state.tasks.currentPage,
        pageSize: state.tasks.pageSize,
        ...params
      }
      const data = await getTaskList(queryParams)
      commit('SET_TASK_LIST', data)
      return data
    } catch (error) {
      console.error('获取任务列表失败:', error)
      throw error
    } finally {
      commit('SET_LOADING', { type: 'tasks', loading: false })
    }
  },

  async fetchTaskDetails({ commit }, taskId) {
    commit('SET_LOADING', { type: 'taskDetails', loading: true })
    try {
      const task = await getTaskDetails(taskId)
      commit('SET_CURRENT_TASK', task)
      return task
    } catch (error) {
      console.error('获取任务详情失败:', error)
      throw error
    } finally {
      commit('SET_LOADING', { type: 'taskDetails', loading: false })
    }
  },

  async addTask({ commit }, taskData) {
    try {
      const task = await createTask(taskData)
      commit('ADD_TASK', task)
      return task
    } catch (error) {
      console.error('添加任务失败:', error)
      throw error
    }
  },

  async updateTask({ commit }, { id, data }) {
    try {
      const task = await updateTask(id, data)
      commit('UPDATE_TASK', task)
      return task
    } catch (error) {
      console.error('更新任务失败:', error)
      throw error
    }
  },

  async removeTask({ commit }, taskId) {
    try {
      await deleteTask(taskId)
      commit('REMOVE_TASK', taskId)
    } catch (error) {
      console.error('删除任务失败:', error)
      throw error
    }
  },

  // 维修人员管理操作
  async fetchRepairmanList({ commit, state }, params = {}) {
    commit('SET_LOADING', { type: 'repairmen', loading: true })
    try {
      const queryParams = {
        page: state.repairmen.currentPage,
        pageSize: state.repairmen.pageSize,
        ...params
      }
      const data = await getRepairmanList(queryParams)
      commit('SET_REPAIRMAN_LIST', data)
      return data
    } catch (error) {
      console.error('获取维修人员列表失败:', error)
      throw error
    } finally {
      commit('SET_LOADING', { type: 'repairmen', loading: false })
    }
  },

  async addRepairman({ commit }, repairmanData) {
    try {
      const repairman = await createRepairman(repairmanData)
      commit('ADD_REPAIRMAN', repairman)
      return repairman
    } catch (error) {
      console.error('添加维修人员失败:', error)
      throw error
    }
  },

  async updateRepairman({ commit }, { id, data }) {
    try {
      const repairman = await updateRepairman(id, data)
      commit('UPDATE_REPAIRMAN', repairman)
      return repairman
    } catch (error) {
      console.error('更新维修人员失败:', error)
      throw error
    }
  },

  async removeRepairman({ commit }, repairmanId) {
    try {
      await deleteRepairman(repairmanId)
      commit('REMOVE_REPAIRMAN', repairmanId)
    } catch (error) {
      console.error('删除维修人员失败:', error)
      throw error
    }
  },

  // 分页操作
  setEquipmentPage({ commit, dispatch }, { currentPage, pageSize }) {
    commit('SET_EQUIPMENT_PAGE', { currentPage, pageSize })
    dispatch('fetchEquipmentList')
  },

  setTaskPage({ commit, dispatch }, { currentPage, pageSize }) {
    commit('SET_TASK_PAGE', { currentPage, pageSize })
    dispatch('fetchTaskList')
  },

  setRepairmanPage({ commit, dispatch }, { currentPage, pageSize }) {
    commit('SET_REPAIRMAN_PAGE', { currentPage, pageSize })
    dispatch('fetchRepairmanList')
  }
}

const getters = {
  // 设备管理
  equipmentList: state => state.equipment.list,
  equipmentTotal: state => state.equipment.total,
  equipmentPagination: state => ({
    currentPage: state.equipment.currentPage,
    pageSize: state.equipment.pageSize,
    total: state.equipment.total
  }),

  // 任务管理
  taskList: state => state.tasks.list,
  taskTotal: state => state.tasks.total,
  currentTask: state => state.tasks.currentTask,
  taskPagination: state => ({
    currentPage: state.tasks.currentPage,
    pageSize: state.tasks.pageSize,
    total: state.tasks.total
  }),
  // 按状态分组的任务
  tasksByStatus: state => {
    const groups = {}
    state.tasks.list.forEach(task => {
      if (!groups[task.status]) {
        groups[task.status] = []
      }
      groups[task.status].push(task)
    })
    return groups
  },

  // 维修人员管理
  repairmanList: state => state.repairmen.list,
  repairmanTotal: state => state.repairmen.total,
  repairmanPagination: state => ({
    currentPage: state.repairmen.currentPage,
    pageSize: state.repairmen.pageSize,
    total: state.repairmen.total
  }),
  // 可用的维修人员
  availableRepairmen: state => state.repairmen.list.filter(repairman => repairman.status === 'available'),

  // 加载状态
  isLoading: state => type => state.loading[type] || false,

  // 表单数据
  getFormData: state => formType => state.forms[formType] || {}
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
}