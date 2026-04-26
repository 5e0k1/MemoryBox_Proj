self.addEventListener('push', function (event) {
  var payload = {
    title: 'MemoryBox',
    body: '새 알림이 도착했습니다.',
    url: '/mypage'
  };

  try {
    if (event.data) {
      var data = event.data.json();
      payload = {
        title: data.title || payload.title,
        body: data.body || payload.body,
        url: data.url || payload.url
      };
    }
  } catch (e) {
    // payload 파싱 실패 시 기본값 사용
  }

  event.waitUntil(
    self.registration.showNotification(payload.title, {
      body: payload.body,
      data: {
        url: payload.url
      }
    })
  );
});

self.addEventListener('notificationclick', function (event) {
  event.notification.close();

  var targetUrl = (event.notification.data && event.notification.data.url) || '/mypage';

  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then(function (windowClients) {
      for (const client of windowClients) {
        if ('navigate' in client) {
          return client.navigate(targetUrl).then(function () {
            return client.focus();
          });
        }
      }
      return clients.openWindow(targetUrl);
    })
  );
});
