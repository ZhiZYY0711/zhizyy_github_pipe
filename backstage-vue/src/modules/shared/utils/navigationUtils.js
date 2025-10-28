import { ROUTE_PATHS, MENU_CONFIG } from './constants.js';

/**
 * 路径判断工具函数
 */
export const PathUtils = {
  /**
   * 判断路径是否在数据可视化模块中
   * @param {string} path - 路径
   * @returns {boolean}
   */
  isPathInVisualization(path) {
    const visualizationPaths = Object.values(ROUTE_PATHS.VISUALIZATION);
    return visualizationPaths.includes(path);
  },

  /**
   * 判断路径是否在数据管理模块中
   * @param {string} path - 路径
   * @returns {boolean}
   */
  isPathInDataManagement(path) {
    const dataManagementPaths = Object.values(ROUTE_PATHS.DATA_MANAGEMENT);
    return dataManagementPaths.includes(path);
  },

  /**
   * 判断是否为当前激活路径
   * @param {string} currentPath - 当前路径
   * @param {string} targetPath - 目标路径
   * @returns {boolean}
   */
  isActivePath(currentPath, targetPath) {
    return currentPath === targetPath;
  }
};

/**
 * 侧边栏管理器类
 */
export class SidebarManager {
  constructor() {
    this.sidebarVisible = false;
    this.waitingForClickToHide = false;
    this.documentClickListener = null;
  }

  /**
   * 显示侧边栏
   * @param {boolean} isVisualizationActive - 是否在数据可视化模块
   */
  showSidebar(isVisualizationActive) {
    if (isVisualizationActive) {
      this.sidebarVisible = true;
      this.waitingForClickToHide = false;
      // 移除可能存在的全局点击监听器
      this.removeDocumentClickListener();
    }
  }

  /**
   * 隐藏侧边栏
   * @param {boolean} isVisualizationActive - 是否在数据可视化模块
   */
  hideSidebar(isVisualizationActive) {
    if (isVisualizationActive) {
      this.sidebarVisible = false;
      this.waitingForClickToHide = false;
      // 移除可能存在的全局点击监听器
      this.removeDocumentClickListener();
    }
  }

  /**
   * 处理鼠标移出侧边栏事件
   * @param {boolean} isVisualizationActive - 是否在数据可视化模块
   * @param {HTMLElement} containerElement - 容器元素
   */
  handleSidebarMouseLeave(isVisualizationActive, containerElement) {
    if (isVisualizationActive) {
      this.waitingForClickToHide = true;
      // 只有在没有监听器的情况下才添加
      if (!this.documentClickListener) {
        this.documentClickListener = (event) => {
          const sidebarElement = containerElement.querySelector('.sidebar');
          const toggleButtonElement = containerElement.querySelector('.sidebar-toggle-btn');
          const noHideElement = event.target && (event.target.closest && event.target.closest('.no-sidebar-hide'));

          // 如果处于等待隐藏状态，且点击目标不在侧边栏和切换按钮内部，则隐藏侧边栏
          if (this.waitingForClickToHide &&
              sidebarElement && !sidebarElement.contains(event.target) &&
              (!toggleButtonElement || !toggleButtonElement.contains(event.target)) &&
              !noHideElement) {
            this.hideSidebar(isVisualizationActive);
          }
        };
        document.body.addEventListener('click', this.documentClickListener);
      }
    }
  }

  /**
   * 移除文档点击监听器
   */
  removeDocumentClickListener() {
    if (this.documentClickListener) {
      document.body.removeEventListener('click', this.documentClickListener);
      this.documentClickListener = null;
    }
  }

  /**
   * 获取当前状态
   */
  getState() {
    return {
      sidebarVisible: this.sidebarVisible,
      waitingForClickToHide: this.waitingForClickToHide
    };
  }

  /**
   * 销毁管理器，清理事件监听器
   */
  destroy() {
    this.removeDocumentClickListener();
  }
}

/**
 * 导航管理器类
 */
export class NavigationManager {
  constructor(router, sidebarManager) {
    this.router = router;
    this.sidebarManager = sidebarManager;
    this.expandedMenus = [MENU_CONFIG.VISUALIZATION];
  }

  /**
   * 导航到指定路径
   * @param {string} path - 目标路径
   * @param {string} currentPath - 当前路径
   */
  navigateTo(path, currentPath) {
    if (currentPath !== path) {
      this.router.push(path);
    }

    // 根据导航路径自动展开对应菜单，并收起另一父级菜单
    if (PathUtils.isPathInVisualization(path)) {
      this.expandVisualizationMenu();
      this.sidebarManager.showSidebar(true);
    } else if (PathUtils.isPathInDataManagement(path)) {
      this.expandDataManagementMenu();
      this.sidebarManager.showSidebar(true);
    } else {
      // 如果导航到非数据可视化和非数据管理相关菜单，收起所有父级菜单
      this.expandedMenus = [];
      this.sidebarManager.hideSidebar(false);
    }
  }

  /**
   * 导航到数据可视化模块
   * @param {string} currentPath - 当前路径
   */
  navigateToVisualization(currentPath) {
    // 确保数据可视化菜单始终展开
    this.expandVisualizationMenu();

    if (currentPath !== ROUTE_PATHS.VISUALIZATION.MAIN) {
      this.router.push(ROUTE_PATHS.VISUALIZATION.MAIN);
    }
    this.sidebarManager.showSidebar(true);
  }

  /**
   * 切换子菜单展开状态
   * @param {string} menuName - 菜单名称
   */
  toggleSubmenu(menuName) {
    // 如果点击的菜单已经展开，则收起；否则展开当前菜单
    if (this.expandedMenus.includes(menuName)) {
      const index = this.expandedMenus.indexOf(menuName);
      this.expandedMenus.splice(index, 1);
    } else {
      // 允许多个父级菜单同时展开
      this.expandedMenus.push(menuName);
    }
  }

  /**
   * 展开数据可视化菜单
   */
  expandVisualizationMenu() {
    if (!this.expandedMenus.includes(MENU_CONFIG.VISUALIZATION)) {
      this.expandedMenus.push(MENU_CONFIG.VISUALIZATION);
    }
    // 收起数据管理菜单
    const dataManagementIndex = this.expandedMenus.indexOf(MENU_CONFIG.DATA_MANAGEMENT);
    if (dataManagementIndex > -1) {
      this.expandedMenus.splice(dataManagementIndex, 1);
    }
  }

  /**
   * 展开数据管理菜单
   */
  expandDataManagementMenu() {
    if (!this.expandedMenus.includes(MENU_CONFIG.DATA_MANAGEMENT)) {
      this.expandedMenus.push(MENU_CONFIG.DATA_MANAGEMENT);
    }
    // 收起数据可视化菜单
    const visualizationIndex = this.expandedMenus.indexOf(MENU_CONFIG.VISUALIZATION);
    if (visualizationIndex > -1) {
      this.expandedMenus.splice(visualizationIndex, 1);
    }
  }

  /**
   * 获取当前状态
   */
  getState() {
    return {
      expandedMenus: [...this.expandedMenus]
    };
  }

  /**
   * 销毁管理器
   */
  destroy() {
    this.sidebarManager.destroy();
  }
}