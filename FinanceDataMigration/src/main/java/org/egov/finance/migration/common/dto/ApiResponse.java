package org.egov.finance.migration.common.dto;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private ResponseInfo responseInfo;

    private boolean success;

    private String message;

    private Date timestamp;

    private T data;

    private static ResponseInfo createResponseInfo(
            RequestInfo requestInfo,
            boolean success) {

        ResponseInfo responseInfo = new ResponseInfo();

        if (requestInfo != null) {

            responseInfo.setApiId(requestInfo.getApiId());
            responseInfo.setMsgId(requestInfo.getMsgId());
            responseInfo.setResMsgId(requestInfo.getMsgId());
            responseInfo.setVer(requestInfo.getVer());

            if (requestInfo.getTs() != null) {
                responseInfo.setTs(requestInfo.getTs().toString());
            }
        }

        responseInfo.setStatus(
                success
                        ? ResponseInfo.StatusEnum.SUCCESSFUL.toString()
                        : ResponseInfo.StatusEnum.FAILED.toString()
        );

        return responseInfo;
    }

    public static <T> ApiResponse<T> error(
            RequestInfo requestInfo,
            String message) {

        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(new Date())
                .responseInfo(
                        createResponseInfo(requestInfo, false)
                )
                .build();
    }

    public static <T> ApiResponse<T> success(
            RequestInfo requestInfo,
            String message,
            T data) {

        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .timestamp(new Date())
                .responseInfo(
                        createResponseInfo(requestInfo, true)
                )
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(
            RequestInfo requestInfo,
            String message) {

        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .timestamp(new Date())
                .responseInfo(
                        createResponseInfo(requestInfo, true)
                )
                .build();
    }
    
    public static <T> ApiResponse<T> error(
            ResponseInfo responseInfo,
            List<Error> errors) {

        String message = errors == null
                ? "Request failed"
                : errors.stream()
                        .map(Error::getMessage)
                        .filter(Objects::nonNull)
                        .filter(msg -> !msg.isBlank())
                        .collect(Collectors.joining("; "));

        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(new Date())
                .responseInfo(responseInfo)
                .build();
    }
}