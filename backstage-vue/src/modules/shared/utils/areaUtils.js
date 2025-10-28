import { API_CONFIG, AUTH_CONFIG, EVENT_NAMES, DEFAULT_VALUES } from './constants.js';

/**
 * 获取认证头信息
 * @returns {Object} 包含认证信息的请求头
 */
export function getAuthHeaders() {
  const token = AUTH_CONFIG.TOKEN_KEYS
    .map(key => localStorage.getItem(key) || sessionStorage.getItem(key))
    .find(token => token);
  
  const headers = {
    'Content-Type': 'application/json'
  };
  
  if (token) {
    // 如果token已经包含Bearer前缀，直接使用；否则添加Bearer前缀
    headers['Authorization'] = token.startsWith(AUTH_CONFIG.BEARER_PREFIX) 
      ? token 
      : `${AUTH_CONFIG.BEARER_PREFIX}${token}`;
  }
  
  return headers;
}

/**
 * 通用的API请求函数
 * @param {string} endpoint - API端点
 * @param {string} param - 可选参数
 * @returns {Promise<Object>} API响应数据
 */
async function fetchAreaData(endpoint, param = '') {
  try {
    const url = param 
      ? `${API_CONFIG.BASE_URL}${endpoint}/${param}`
      : `${API_CONFIG.BASE_URL}${endpoint}`;
    
    const response = await fetch(url, {
      method: 'GET',
      headers: getAuthHeaders()
    });
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    
    const result = await response.json();
    if (result.code === 200) {
      return result.data;
    } else {
      console.error('API请求失败:', result.message);
      return [];
    }
  } catch (error) {
    console.error('API请求出错:', error);
    return [];
  }
}

/**
 * 加载省份数据
 * @returns {Promise<Array>} 省份列表
 */
export async function loadProvinces() {
  return await fetchAreaData(API_CONFIG.ENDPOINTS.PROVINCES);
}

/**
 * 加载城市数据
 * @param {string} provinceCode - 省份代码
 * @returns {Promise<Array>} 城市列表
 */
export async function loadCities(provinceCode) {
  return await fetchAreaData(API_CONFIG.ENDPOINTS.CITIES, provinceCode);
}

/**
 * 加载区县数据
 * @param {string} cityCode - 城市代码
 * @returns {Promise<Array>} 区县列表
 */
export async function loadDistricts(cityCode) {
  return await fetchAreaData(API_CONFIG.ENDPOINTS.DISTRICTS, cityCode);
}

/**
 * 创建区域管理器类
 */
export class AreaManager {
  constructor(vueInstance) {
    this.vue = vueInstance;
    this.provinces = [];
    this.cities = [];
    this.districts = [];
    this.selectedProvince = DEFAULT_VALUES.EMPTY_STRING;
    this.selectedCity = DEFAULT_VALUES.EMPTY_STRING;
    this.selectedDistrict = DEFAULT_VALUES.EMPTY_STRING;
    this.currentAreaIds = {
      provinceId: null,
      cityId: null,
      districtId: null
    };
  }

  /**
   * 初始化区域数据
   */
  async init() {
    this.provinces = await loadProvinces();
  }

  /**
   * 省份选择变化处理
   * @param {string} provinceCode - 选中的省份代码
   */
  async onProvinceChange(provinceCode) {
    this.selectedProvince = provinceCode;
    
    // 重置城市和区县
    this.selectedCity = DEFAULT_VALUES.EMPTY_STRING;
    this.selectedDistrict = DEFAULT_VALUES.EMPTY_STRING;
    this.cities = [];
    this.districts = [];
    
    if (provinceCode) {
      // 加载对应的城市数据
      this.cities = await loadCities(provinceCode);
      this.currentAreaIds.provinceId = provinceCode;
    } else {
      this.currentAreaIds.provinceId = null;
    }
    
    // 重置城市和区县的area_id
    this.currentAreaIds.cityId = null;
    this.currentAreaIds.districtId = null;
    
    // 触发全局事件
    this.emitAreaChange();
  }

  /**
   * 城市选择变化处理
   * @param {string} cityCode - 选中的城市代码
   */
  async onCityChange(cityCode) {
    this.selectedCity = cityCode;
    
    // 重置区县
    this.selectedDistrict = DEFAULT_VALUES.EMPTY_STRING;
    this.districts = [];
    
    if (cityCode) {
      // 加载对应的区县数据
      this.districts = await loadDistricts(cityCode);
      this.currentAreaIds.cityId = cityCode;
    } else {
      this.currentAreaIds.cityId = null;
    }
    
    // 重置区县的area_id
    this.currentAreaIds.districtId = null;
    
    // 触发全局事件
    this.emitAreaChange();
  }

  /**
   * 区县选择变化处理
   * @param {string} districtCode - 选中的区县代码
   */
  onDistrictChange(districtCode) {
    this.selectedDistrict = districtCode;
    
    if (districtCode) {
      this.currentAreaIds.districtId = districtCode;
    } else {
      this.currentAreaIds.districtId = null;
    }
    
    // 触发全局事件
    this.emitAreaChange();
  }

  /**
   * 触发区域变化事件
   */
  emitAreaChange() {
    const eventData = {
      provinceId: this.currentAreaIds.provinceId,
      cityId: this.currentAreaIds.cityId,
      districtId: this.currentAreaIds.districtId,
      provinceName: this.selectedProvince ? this.provinces.find(p => p.code === this.selectedProvince)?.name : DEFAULT_VALUES.EMPTY_STRING,
      cityName: this.selectedCity ? this.cities.find(c => c.code === this.selectedCity)?.name : DEFAULT_VALUES.EMPTY_STRING,
      districtName: this.selectedDistrict ? this.districts.find(d => d.code === this.selectedDistrict)?.name : DEFAULT_VALUES.EMPTY_STRING
    };
    
    // 使用Vue的事件总线触发事件
    this.vue.$root.$emit(EVENT_NAMES.AREA_CHANGED, eventData);
  }

  /**
   * 获取当前状态
   */
  getState() {
    return {
      provinces: this.provinces,
      cities: this.cities,
      districts: this.districts,
      selectedProvince: this.selectedProvince,
      selectedCity: this.selectedCity,
      selectedDistrict: this.selectedDistrict,
      currentAreaIds: { ...this.currentAreaIds }
    };
  }
}