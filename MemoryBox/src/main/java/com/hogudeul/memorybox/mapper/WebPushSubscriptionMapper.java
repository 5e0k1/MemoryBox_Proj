package com.hogudeul.memorybox.mapper;

import com.hogudeul.memorybox.model.WebPushSubscription;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WebPushSubscriptionMapper {

    int insert(WebPushSubscription subscription);

    int update(WebPushSubscription subscription);

    WebPushSubscription findByEndpoint(@Param("endpoint") String endpoint);

    List<WebPushSubscription> findByUserId(@Param("userId") Long userId);
}
