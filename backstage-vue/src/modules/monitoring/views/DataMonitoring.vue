<template>
  <div class="data-monitoring">
    <transition name="page-fade" mode="out-in">
      <!-- 根据当前页面类型切换组件，并应用淡入淡出过渡效果 -->
      <component
        :is="currentPage === 'area' ? 'AreaDetails' : 'PipelineDetails'"
        :key="currentPage"
        @switch-page="switchPage"
        @switch-to-area="switchToArea"
      />
    </transition>
  </div>
</template>

<script>
import AreaDetails from '@dashboard/views/AreaDetails.vue'
import PipelineDetails from '@dashboard/views/PipelineDetails.vue'

export default {
  name: 'DataMonitoring',
  components: {
    AreaDetails,
    PipelineDetails
  },
  data() {
    return {
      currentPage: 'area' // 默认显示区域详情
    }
  },
  methods: {
    switchPage(pageType) {
      this.currentPage = pageType
    },
    
    switchToArea() {
      this.currentPage = 'area'
    }
  }
}
</script>

<style scoped>
.data-monitoring {
  height: 100%;
  width: 100%;
}
</style>