self.addEventListener('push', function (event) {
  event.waitUntil(
    self.registration.showNotification('MemoryBox 테스트', {
      body: 'Service Worker push 이벤트가 정상 동작했습니다.',
      data: {
        url: '/mypage'
      }
    })
  );
});

self.addEventListener('notificationclick', function (event) {
  event.notification.close();

  event.waitUntil(
    clients.openWindow((event.notification.data && event.notification.data.url) || '/mypage')
  );
});
