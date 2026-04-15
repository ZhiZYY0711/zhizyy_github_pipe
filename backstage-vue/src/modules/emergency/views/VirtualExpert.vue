<template>
  <div class="virtual-expert-container">
    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 左侧：历史记录列表 -->
      <div class="left-panel">
        <div class="history-header">
          <h3 class="history-title">聊天历史</h3>
          <button class="btn btn-primary btn-sm" @click="startNewChat">
            <i class="icon">+</i> 新建对话
          </button>
        </div>

        <div class="history-list">
          <div v-if="loadingHistory" class="loading-indicator">
            <div class="loading-spinner"></div>
            <span>加载聊天历史中...</span>
          </div>
          <div v-else-if="chatHistory.length === 0" class="empty-history">
            <span>暂无聊天历史</span>
          </div>
          <div
            v-else
            v-for="session in chatHistory"
            :key="session.id"
            class="history-item"
            :class="{ active: selectedSessionId === session.id }"
            @click="selectSession(session)"
          >
            <div class="session-info">
              <div class="session-title">{{ session.title || "新对话" }}</div>
              <div class="session-time">
                {{ formatTime(session.startTime) }}
              </div>
            </div>
            <div class="session-preview">{{ session.lastMessage }}</div>
            <div class="session-meta">
              <span class="message-count"
                >{{ session.messageCount }} 条消息</span
              >
              <span class="session-status" :class="session.status">
                {{ session.status === "active" ? "进行中" : "已结束" }}
              </span>
            </div>
          </div>
        </div>

        <!-- 连接状态 -->
        <div class="connection-status-panel">
          <div
            class="connection-status"
            :class="{ connected: isConnected, disconnected: !isConnected }"
          >
            <span class="status-indicator"></span>
            <span class="status-text">{{
              isConnected ? "AI专家在线" : "AI专家离线"
            }}</span>
          </div>
          <button
            v-if="!isConnected"
            class="btn btn-secondary btn-sm"
            @click="connectWebSocket"
          >
            重新连接
          </button>
        </div>
      </div>

      <!-- 右侧：聊天对话框 -->
      <div class="right-panel">
        <div class="chat-container">
          <!-- 聊天头部 -->
          <div class="chat-header">
            <div class="chat-info">
              <div class="expert-avatar">
                <img src="/ai-expert-avatar.png" alt="AI专家" />
              </div>
              <div class="expert-details">
                <div class="expert-name">AI智能专家</div>
                <div class="expert-specialty">管道监测全领域专家</div>
              </div>
            </div>
            <div class="chat-actions">
              <button
                class="btn btn-secondary btn-sm"
                @click="clearCurrentChat"
                :disabled="!selectedSessionId"
              >
                清空对话
              </button>
              <button
                class="btn btn-primary btn-sm"
                @click="saveCurrentChat"
                :disabled="!selectedSessionId"
              >
                保存对话
              </button>
            </div>
          </div>

          <!-- 聊天消息区域 -->
          <div class="chat-messages" ref="chatMessages">
            <div v-if="!selectedSessionId" class="welcome-screen">
              <div class="welcome-content">
                <div class="welcome-icon">🤖</div>
                <h3>欢迎使用AI虚拟专家系统</h3>
                <p>选择历史对话或开始新的对话</p>
                <div v-if="!isConnected" class="connection-warning">
                  <p>⚠️ 当前未连接到AI服务</p>
                  <button class="btn btn-primary" @click="connectWebSocket">
                    连接AI服务
                  </button>
                </div>
              </div>
            </div>

            <div v-else class="messages-list">
              <div v-if="loadingMessages" class="loading-indicator">
                <div class="loading-spinner"></div>
                <span>加载消息中...</span>
              </div>
              <div
                v-else
                v-for="message in currentMessages"
                :key="message.id"
                class="message"
                :class="[message.type, message.sender]"
              >
                <div class="message-avatar">
                  <img
                    :src="getMessageAvatar(message)"
                    :alt="getMessageSender(message)"
                  />
                </div>
                <div class="message-content">
                  <div class="message-header">
                    <span class="message-sender">{{
                      getMessageSender(message)
                    }}</span>
                    <span class="message-time">{{
                      formatMessageTime(message.timestamp)
                    }}</span>
                  </div>
                  <div class="message-text">{{ message.content }}</div>
                </div>
              </div>

              <!-- AI正在输入指示器 -->
              <div v-if="aiTyping" class="message ai typing-indicator">
                <div class="message-avatar">
                  <img src="/ai-expert-avatar.png" alt="AI专家" />
                </div>
                <div class="message-content">
                  <div class="typing-animation">
                    <span class="typing-text">AI正在思考</span>
                    <div class="typing-dots">
                      <span></span>
                      <span></span>
                      <span></span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 输入区域 -->
          <div class="chat-input">
            <div class="input-container">
              <textarea
                v-model="newMessage"
                placeholder="请输入您的问题..."
                class="message-input"
                rows="3"
                @keydown.enter.prevent="handleEnterKey"
                :disabled="!isConnected"
              ></textarea>
              <div class="input-actions">
                <button
                  class="btn btn-primary"
                  @click="sendMessage"
                  :disabled="!newMessage.trim() || !isConnected"
                >
                  发送
                </button>
              </div>
            </div>
            <div v-if="!isConnected" class="connection-hint">
              请先连接AI服务才能发送消息
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "VirtualExpert",
  data() {
    return {
      // SSE连接相关
      eventSource: null,
      isConnected: false,
      reconnectAttempts: 0,
      maxReconnectAttempts: 5,
      reconnectInterval: 3000,
      currentAiMessageId: null,
      connectedConversationId: null,

      // 聊天相关
      selectedSessionId: null,
      currentMessages: [],
      newMessage: "",
      aiTyping: false,

      // 历史记录
      chatHistory: [],

      // 当前会话信息
      currentSessionInfo: null,

      // 加载状态
      loadingHistory: false,
      loadingMessages: false,
    };
  },
  mounted() {
    // 组件挂载后自动连接SSE和加载历史记录
    this.connectWebSocket();
    this.loadChatHistory();
  },
  beforeUnmount() {
    // 组件销毁前断开SSE连接
    this.disconnectWebSocket();
  },
  methods: {
    // 获取JWT token
    getJwtToken() {
      return (
        this.$store?.getters?.["auth/token"] ||
        localStorage.getItem("token") ||
        localStorage.getItem("jwt")
      );
    },

    // SSE连接管理（保留原方法名，避免模板改动）
    connectWebSocket() {
      if (this.eventSource && this.isConnected) {
        console.log("SSE已连接");
        return;
      }

      try {
        const protocol = window.location.protocol;
        const host = window.location.hostname;
        const port = "8080";
        const conversationId = this.selectedSessionId || `session_${Date.now()}`;
        const userId = localStorage.getItem("username") || "web_user";
        const token = this.getJwtToken();
        const sseUrl = `${protocol}//${host}:${port}/manager/virtual-expert/stream?conversationId=${encodeURIComponent(
          conversationId
        )}&userId=${encodeURIComponent(userId)}${
          token ? `&token=${encodeURIComponent(token)}` : ""
        }`;

        if (this.eventSource) {
          this.eventSource.close();
        }
        this.eventSource = new EventSource(sseUrl);
        this.selectedSessionId = conversationId;
        this.connectedConversationId = conversationId;
        this.eventSource.onopen = () => this.onSseOpen();
        this.eventSource.onerror = (error) => this.onSseError(error);
        ["status", "delta", "done", "error", "system"].forEach((eventType) => {
          this.eventSource.addEventListener(eventType, (event) =>
            this.handleSseEvent(eventType, event)
          );
        });
      } catch (error) {
        console.error("SSE连接失败:", error);
        this.isConnected = false;
        this.$message.error("连接AI服务失败");
      }
    },

    disconnectWebSocket() {
      if (this.eventSource) {
        this.eventSource.close();
        this.eventSource = null;
      }
      this.isConnected = false;
      this.connectedConversationId = null;
    },

    onSseOpen() {
      console.log("SSE连接已建立");
      this.isConnected = true;
      this.reconnectAttempts = 0;
      this.$message.success("AI服务连接成功");
    },

    onSseError(error) {
      console.error("SSE连接错误:", error);
      this.isConnected = false;
      if (this.eventSource) {
        this.eventSource.close();
        this.eventSource = null;
      }
      if (this.reconnectAttempts < this.maxReconnectAttempts) {
        setTimeout(() => {
          this.reconnectAttempts++;
          this.connectWebSocket();
        }, this.reconnectInterval);
      } else {
        this.$message.error("AI服务连接失败，请手动重新连接");
      }
    },

    handleSseEvent(eventType, event) {
      try {
        const payload = JSON.parse(event.data || "{}");
        const content = payload.content || "";

        if (eventType === "status") {
          if (content && content.includes("思考")) {
            this.aiTyping = true;
          }
          return;
        }

        if (eventType === "delta") {
          this.aiTyping = true;
          this.appendAiDelta(content);
          return;
        }

        if (eventType === "done") {
          this.aiTyping = false;
          this.currentAiMessageId = null;
          return;
        }

        if (eventType === "error") {
          this.aiTyping = false;
          this.currentAiMessageId = null;
          this.addMessage("error", content || "AI回复出现错误");
          this.$message.error("AI回复出现错误");
          return;
        }

        if (eventType === "system") {
          this.addMessage("system", content);
        }
      } catch (error) {
        console.error("解析SSE消息失败:", error);
      }
    },

    appendAiDelta(content) {
      if (!content) return;
      const lastMessage =
        this.currentAiMessageId &&
        this.currentMessages.find((msg) => msg.id === this.currentAiMessageId);
      if (lastMessage) {
        lastMessage.content += content;
        lastMessage.timestamp = Date.now();
      } else {
        const message = {
          id: Date.now() + Math.random(),
          type: "ai",
          content: content,
          timestamp: Date.now(),
          sender: "ai",
        };
        this.currentMessages.push(message);
        this.currentAiMessageId = message.id;
      }
      this.$nextTick(() => {
        this.scrollToBottom();
      });
    },

    // 消息发送
    sendMessage() {
      if (!this.newMessage.trim() || !this.isConnected) return;

      // 如果没有选中会话，创建新会话
      if (!this.selectedSessionId) {
        this.startNewChat();
      }

      // 添加用户消息
      this.addMessage("user", this.newMessage);
      const userMessage = this.newMessage;
      this.newMessage = "";

      // 发送到AI
      this.sendToAI(userMessage);
    },

    async sendToAI(message) {
      if (!this.isConnected) {
        this.$message.error("AI服务未连接，请稍后重试");
        return;
      }

      if (this.connectedConversationId !== this.selectedSessionId) {
        this.disconnectWebSocket();
        this.connectWebSocket();
      }

      try {
        const protocol = window.location.protocol;
        const host = window.location.hostname;
        const port = "8080";
        const payload = {
          conversationId: this.selectedSessionId,
          userId: localStorage.getItem("username") || "web_user",
          type: "chat",
          content: message,
        };
        const headers = {
          "Content-Type": "application/json",
        };
        const token = this.getJwtToken();
        if (token) {
          headers.Authorization = `Bearer ${token}`;
        }

        const response = await fetch(`${protocol}//${host}:${port}/manager/virtual-expert/messages`, {
          method: "POST",
          headers,
          body: JSON.stringify(payload),
        });
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`);
        }
        const result = await response.json();
        if (!result.success) {
          throw new Error(result.message || "发送失败");
        }
        this.aiTyping = true;
        this.currentAiMessageId = null;
      } catch (error) {
        console.error("发送消息到AI失败:", error);
        this.$message.error("发送消息失败，请重试");
      }
    },

    // 消息处理
    addMessage(type, content) {
      const message = {
        id: Date.now() + Math.random(),
        type,
        content,
        timestamp: Date.now(),
        sender: type === "user" ? "user" : type === "ai" ? "ai" : "system",
      };

      this.currentMessages.push(message);

      // 更新当前会话的最后消息
      if (this.selectedSessionId) {
        this.updateSessionLastMessage(content, type);
      }

      this.$nextTick(() => {
        this.scrollToBottom();
      });
    },

    // 会话管理
    startNewChat() {
      const newSession = {
        id: "session_" + Date.now(),
        title: "新对话",
        startTime: Date.now(),
        lastMessage: "",
        messageCount: 0,
        status: "active",
      };

      this.chatHistory.unshift(newSession);
      this.selectedSessionId = newSession.id;
      this.currentMessages = [];
      this.disconnectWebSocket();
      this.connectWebSocket();

      // 添加欢迎消息
      if (this.isConnected) {
        this.addMessage(
          "ai",
          "您好！我是AI智能专家，专业领域覆盖管道监测全领域。请问有什么可以帮助您的？"
        );
      }
    },

    selectSession(session) {
      this.selectedSessionId = session.id;
      // 从后端加载具体的消息历史
      this.loadSessionMessages(session.id);
    },

    async loadSessionMessages(sessionId) {
      this.loadingMessages = true;
      try {
        // 找到对应的会话记录
        const session = this.chatHistory.find((s) => s.id === sessionId);
        if (!session || !session.fileName) {
          console.error("未找到会话记录或文件名");
          this.currentMessages = [];
          return;
        }

        // 准备请求头，包含认证信息
        const headers = {
          "Content-Type": "application/json",
        };

        // 添加认证头
        const token = this.getJwtToken();
        if (token) {
          headers["Authorization"] = `Bearer ${token}`;
        }

        // 调用后端API获取聊天记录内容
        const response = await fetch(
          `http://localhost:8080/manager/chat-logs/${encodeURIComponent(
            session.fileName
          )}`,
          {
            method: "GET",
            headers,
          }
        );

        const result = await response.json();

        if (result.success) {
          // 解析聊天记录内容
          const content = result.content;
          const messages = this.parseChatContent(content);

          this.currentMessages = messages;

          // 更新会话的消息数量
          session.messageCount = messages.length;

          console.log("成功加载会话消息:", messages.length, "条");
        } else {
          console.error("获取聊天记录内容失败:", result.message);
          this.$message.error("获取聊天记录内容失败: " + result.message);
          this.currentMessages = [];
        }
      } catch (error) {
        console.error("加载会话消息失败:", error);
        this.$message.error("加载会话消息失败，请检查网络连接");
        this.currentMessages = [];
      } finally {
        this.loadingMessages = false;
      }
    },

    // 解析聊天记录内容，将文本转换为消息对象数组
    parseChatContent(content) {
      if (!content || typeof content !== "string") {
        return [];
      }

      const messages = [];
      const lines = content.split("\n").filter((line) => line.trim());

      let currentMessage = null;
      let messageId = 1;

      for (const line of lines) {
        const trimmedLine = line.trim();

        // 检查是否是时间戳行（格式：[2024-01-01 12:00:00]）
        const timestampMatch = trimmedLine.match(
          /^\[(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})\]/
        );
        if (timestampMatch) {
          // 如果有当前消息，先保存它
          if (currentMessage) {
            messages.push(currentMessage);
          }

          // 解析时间戳
          const timestamp = new Date(timestampMatch[1]).getTime();

          // 检查发送者（用户或AI）
          const remainingText = trimmedLine
            .substring(timestampMatch[0].length)
            .trim();
          let sender = "ai";
          let content = remainingText;

          if (
            remainingText.startsWith("用户:") ||
            remainingText.startsWith("User:")
          ) {
            sender = "user";
            content = remainingText
              .substring(remainingText.indexOf(":") + 1)
              .trim();
          } else if (
            remainingText.startsWith("AI:") ||
            remainingText.startsWith("助手:")
          ) {
            sender = "ai";
            content = remainingText
              .substring(remainingText.indexOf(":") + 1)
              .trim();
          }

          currentMessage = {
            id: messageId++,
            type: sender,
            content: content,
            timestamp: timestamp || Date.now(),
            sender: sender,
          };
        } else if (currentMessage) {
          // 如果不是时间戳行，且有当前消息，则追加到当前消息内容
          currentMessage.content += "\n" + trimmedLine;
        } else {
          // 如果没有时间戳格式，创建一个默认消息
          currentMessage = {
            id: messageId++,
            type: "ai",
            content: trimmedLine,
            timestamp: Date.now(),
            sender: "ai",
          };
        }
      }

      // 保存最后一条消息
      if (currentMessage) {
        messages.push(currentMessage);
      }

      // 如果没有解析到任何消息，创建一个默认消息显示原始内容
      if (messages.length === 0 && content.trim()) {
        messages.push({
          id: 1,
          type: "ai",
          content: content.trim(),
          timestamp: Date.now(),
          sender: "ai",
        });
      }

      return messages;
    },

    updateSessionLastMessage(content, type) {
      const session = this.chatHistory.find(
        (s) => s.id === this.selectedSessionId
      );
      if (session) {
        session.lastMessage =
          content.length > 50 ? content.substring(0, 50) + "..." : content;
        session.messageCount = this.currentMessages.length;

        // 如果是用户消息且没有标题，用第一条用户消息作为标题
        if (type === "user" && session.title === "新对话") {
          session.title =
            content.length > 20 ? content.substring(0, 20) + "..." : content;
        }
      }
    },

    clearCurrentChat() {
      if (this.selectedSessionId) {
        this.currentMessages = [];
        const session = this.chatHistory.find(
          (s) => s.id === this.selectedSessionId
        );
        if (session) {
          session.messageCount = 0;
          session.lastMessage = "";
        }
      }
    },

    saveCurrentChat() {
      if (this.selectedSessionId) {
        // 这里应该调用API保存到后端
        this.$message.success("对话已保存");
      }
    },

    // 历史记录管理
    async loadChatHistory() {
      this.loadingHistory = true;
      try {
        // 准备请求头，包含认证信息
        const headers = {
          "Content-Type": "application/json",
        };

        // 添加认证头
        const token = this.getJwtToken();
        if (token) {
          headers["Authorization"] = `Bearer ${token}`;
        }

        // 调用后端API获取聊天记录列表
        const response = await fetch(
          "http://localhost:8080/manager/chat-logs",
          {
            method: "GET",
            headers,
          }
        );

        const result = await response.json();

        if (result.success) {
          // 转换后端数据格式为前端需要的格式
          this.chatHistory = result.fileList
            .map((file) => {
              // 解析文件名获取信息
              let title = "聊天记录";
              let startTime = Date.now();

              // 如果文件名包含时间戳信息，解析它
              if (file.timestamp) {
                try {
                  startTime = parseInt(file.timestamp);
                } catch (e) {
                  console.warn("解析时间戳失败:", file.timestamp);
                }
              }

              // 如果有sessionId，使用它作为标题的一部分
              if (file.sessionId) {
                title = `对话 ${file.sessionId.substring(0, 8)}`;
              }

              return {
                id: file.sessionId || file.fileName,
                title: title,
                startTime: startTime,
                lastMessage: "点击查看详细内容",
                messageCount: 0, // 需要加载具体内容后才能知道
                status: "completed",
                fileName: file.fileName, // 保存文件名用于后续加载内容
                fileSize: file.size,
                lastModified: file.lastModified,
              };
            })
            .sort((a, b) => b.startTime - a.startTime); // 按时间倒序排列

          console.log("成功加载聊天历史:", this.chatHistory.length, "条记录");
        } else {
          console.error("获取聊天历史失败:", result.message);
          this.$message.error("获取聊天历史失败: " + result.message);
          // 如果获取失败，使用空数组
          this.chatHistory = [];
        }
      } catch (error) {
        console.error("加载聊天历史失败:", error);
        this.$message.error("加载聊天历史失败，请检查网络连接");
        // 如果请求失败，使用空数组
        this.chatHistory = [];
      } finally {
        this.loadingHistory = false;
      }
    },

    // 工具方法
    getMessageAvatar(message) {
      if (message.sender === "user") {
        return "/default-user-avatar.png";
      } else if (message.sender === "ai") {
        return "/ai-expert-avatar.png";
      }
      return "/system-avatar.png";
    },

    getMessageSender(message) {
      if (message.sender === "user") {
        return "您";
      } else if (message.sender === "ai") {
        return "AI专家";
      }
      return "系统";
    },

    formatTime(timestamp) {
      const date = new Date(timestamp);
      const now = new Date();
      const diff = now - date;

      if (diff < 60000) {
        // 1分钟内
        return "刚刚";
      } else if (diff < 3600000) {
        // 1小时内
        return Math.floor(diff / 60000) + "分钟前";
      } else if (diff < 86400000) {
        // 1天内
        return Math.floor(diff / 3600000) + "小时前";
      } else {
        return date.toLocaleDateString();
      }
    },

    formatMessageTime(timestamp) {
      return new Date(timestamp).toLocaleTimeString();
    },

    scrollToBottom() {
      const messagesContainer = this.$refs.chatMessages;
      if (messagesContainer) {
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
      }
    },

    handleEnterKey(event) {
      if (!event.shiftKey) {
        this.sendMessage();
      }
    },
  },
};
</script>

<style scoped>
.virtual-expert-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.main-content {
  flex: 1;
  display: flex;
  height: calc(100vh - 60px);
}

/* 左侧历史记录面板 */
.left-panel {
  width: 320px;
  background: white;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
}

.history-header {
  padding: 20px;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.history-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.history-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.history-item {
  padding: 15px;
  margin-bottom: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.history-item:hover {
  background: #f8f9fa;
  border-color: #e3f2fd;
}

.history-item.active {
  background: #e3f2fd;
  border-color: #2196f3;
}

.session-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.session-title {
  font-weight: 500;
  color: #333;
  font-size: 14px;
}

.session-time {
  font-size: 12px;
  color: #666;
}

.session-preview {
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
  line-height: 1.4;
}

.session-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}

.message-count {
  color: #999;
}

.session-status {
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
}

.session-status.active {
  background: #e8f5e8;
  color: #4caf50;
}

.session-status.completed {
  background: #f0f0f0;
  color: #666;
}

.connection-status-panel {
  padding: 15px 20px;
  border-top: 1px solid #e0e0e0;
  background: #fafafa;
}

.connection-status {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.status-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 8px;
}

.connection-status.connected .status-indicator {
  background: #4caf50;
}

.connection-status.disconnected .status-indicator {
  background: #f44336;
}

.status-text {
  font-size: 13px;
  color: #666;
}

/* 右侧聊天面板 */
.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.chat-header {
  padding: 20px;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
}

.chat-info {
  display: flex;
  align-items: center;
}

.expert-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-right: 12px;
  overflow: hidden;
}

.expert-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.expert-name {
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.expert-specialty {
  font-size: 13px;
  color: #666;
}

.chat-actions {
  display: flex;
  gap: 10px;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #fafafa;
}

.welcome-screen {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.welcome-content {
  text-align: center;
  color: #666;
}

.welcome-icon {
  font-size: 48px;
  margin-bottom: 20px;
}

.welcome-content h3 {
  margin-bottom: 10px;
  color: #333;
}

.connection-warning {
  margin-top: 20px;
  padding: 15px;
  background: #fff3cd;
  border: 1px solid #ffeaa7;
  border-radius: 8px;
  color: #856404;
}

.messages-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.message {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}

.message-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.message-content {
  flex: 1;
  max-width: 70%;
}

.message.user .message-content {
  text-align: right;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 5px;
  font-size: 12px;
  color: #666;
}

.message.user .message-header {
  flex-direction: row-reverse;
}

.message-text {
  background: white;
  padding: 12px 16px;
  border-radius: 18px;
  line-height: 1.4;
  word-wrap: break-word;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.message.user .message-text {
  background: #2196f3;
  color: white;
}

.message.ai .message-text {
  background: #f0f0f0;
}

.message.system .message-text {
  background: #fff3cd;
  color: #856404;
}

.typing-indicator .message-content {
  background: #f0f0f0;
  padding: 12px 16px;
  border-radius: 18px;
}

.typing-animation {
  display: flex;
  align-items: center;
  gap: 8px;
}

.typing-text {
  font-size: 13px;
  color: #666;
}

.typing-dots {
  display: flex;
  gap: 3px;
}

.typing-dots span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #999;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-dots span:nth-child(1) {
  animation-delay: -0.32s;
}
.typing-dots span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes typing {
  0%,
  80%,
  100% {
    transform: scale(0);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

/* 加载指示器样式 */
.loading-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  color: #666;
  font-size: 14px;
}

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid #f3f3f3;
  border-top: 2px solid #2196f3;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-right: 10px;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.empty-history {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #999;
  font-size: 14px;
  text-align: center;
}

.chat-input {
  padding: 20px;
  border-top: 1px solid #e0e0e0;
  background: white;
}

.input-container {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.message-input {
  flex: 1;
  border: 1px solid #ddd;
  border-radius: 20px;
  padding: 12px 16px;
  resize: none;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.4;
  outline: none;
  transition: border-color 0.2s;
}

.message-input:focus {
  border-color: #2196f3;
}

.message-input:disabled {
  background: #f5f5f5;
  color: #999;
}

.input-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.connection-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #f44336;
  text-align: center;
}

/* 按钮样式 */
.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  background: #2196f3;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #1976d2;
}

.btn-secondary {
  background: #f5f5f5;
  color: #666;
}

.btn-secondary:hover:not(:disabled) {
  background: #e0e0e0;
}

.btn-sm {
  padding: 6px 12px;
  font-size: 12px;
}

.icon {
  font-style: normal;
}

/* 滚动条样式 */
.history-list::-webkit-scrollbar,
.chat-messages::-webkit-scrollbar {
  width: 6px;
}

.history-list::-webkit-scrollbar-track,
.chat-messages::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.history-list::-webkit-scrollbar-thumb,
.chat-messages::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.history-list::-webkit-scrollbar-thumb:hover,
.chat-messages::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .left-panel {
    width: 280px;
  }

  .message-content {
    max-width: 85%;
  }
}
</style>