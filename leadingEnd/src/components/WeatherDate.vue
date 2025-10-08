<template>
  <div class="weather-date">
    <div class="weather-info">
      <i class="weather-icon">☀️</i> <!-- 示例图标，实际应根据天气数据动态显示 -->
      <span class="temperature">25°C</span>
      <span class="weather-description">晴</span>
    </div>
    <div class="date-info">
      <span class="current-date">{{ currentDate }}</span>
      <span class="current-time">{{ currentTime }}</span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'WeatherDate',
  data() {
    return {
      currentDate: '',
      currentTime: '',
      weather: {
        temperature: '25°C',
        description: '晴',
        icon: '☀️'
      }
    };
  },
  mounted() {
    this.updateDateTime();
    setInterval(this.updateDateTime, 1000); // 每秒更新时间
    // 实际项目中，这里会调用API获取天气数据
    // this.fetchWeatherData(); 
  },
  methods: {
    updateDateTime() {
      const now = new Date();
      const optionsDate = { year: 'numeric', month: 'long', day: 'numeric' };
      const optionsTime = { hour: '2-digit', minute: '2-digit', second: '2-digit' };
      this.currentDate = now.toLocaleDateString('zh-CN', optionsDate);
      this.currentTime = now.toLocaleTimeString('zh-CN', optionsTime);
    },
    fetchWeatherData() {
      // 实际天气API调用逻辑
      // 例如：
      // fetch('https://api.weatherapi.com/v1/current.json?key=YOUR_API_KEY&q=Beijing')
      //   .then(response => response.json())
      //   .then(data => {
      //     this.weather.temperature = `${data.current.temp_c}°C`;
      //     this.weather.description = data.current.condition.text;
      //     // 根据天气描述设置图标
      //     if (data.current.condition.text.includes('晴')) {
      //       this.weather.icon = '☀️';
      //     } else if (data.current.condition.text.includes('云')) {
      //       this.weather.icon = '☁️';
      //     } else if (data.current.condition.text.includes('雨')) {
      //       this.weather.icon = '🌧️';
      //     }
      //   })
      //   .catch(error => console.error('Error fetching weather data:', error));
    }
  }
};
</script>

<style scoped>
.weather-date {
  display: flex;
  align-items: center;
  gap: 10px; /* 调整天气信息和日期信息之间的间距 */
  color: #E6F0FF;
  font-size: 14px;
}

.weather-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px; /* 调整padding */
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.weather-icon {
  font-size: 20px;
}

.temperature {
  font-weight: bold;
}

.date-info {
  display: flex;
  flex-direction: column; /* 改回垂直排列 */
  align-items: flex-end; /* 右对齐 */
  gap: 2px; /* 调整间距 */
}

.current-date {
  font-weight: 500;
  font-size: 13px; /* 调整字体大小 */
}

.current-time {
  font-size: 11px; /* 调整字体大小 */
  opacity: 0.8;
}
</style>