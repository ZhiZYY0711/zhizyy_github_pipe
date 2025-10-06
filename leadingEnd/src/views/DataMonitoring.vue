<template>
  <div class="data-monitoring">
    <!-- 区域详情页面 -->
    <AreaDetails 
      v-if="currentPage === 'area'" 
      @switch-page="switchPage"
    />
    
    <!-- 管道详情页面 -->
    <PipelineDetails 
      v-if="currentPage === 'pipeline'" 
      @switch-page="switchPage"
    />
  </div>
</template>

<script>
import AreaDetails from './AreaDetails.vue'
import PipelineDetails from './PipelineDetails.vue'

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