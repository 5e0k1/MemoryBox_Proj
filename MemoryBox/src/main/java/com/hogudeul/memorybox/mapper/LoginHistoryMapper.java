package com.hogudeul.memorybox.mapper;

import com.hogudeul.memorybox.model.LoginHistory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoginHistoryMapper {

    Long selectNextLoginHistoryId();

    int insert(LoginHistory loginHistory);
}
