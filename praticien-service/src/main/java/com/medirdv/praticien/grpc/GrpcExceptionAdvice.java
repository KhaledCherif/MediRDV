package com.medirdv.praticien.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;
import com.medirdv.praticien.exception.ConflictException;
import com.medirdv.praticien.exception.NotFoundException;

@GrpcAdvice
public class GrpcExceptionAdvice {

    @GrpcExceptionHandler
    public StatusRuntimeException notFound(NotFoundException exception) {
        return Status.NOT_FOUND.withDescription(exception.getMessage()).asRuntimeException();
    }

    @GrpcExceptionHandler
    public StatusRuntimeException failedPrecondition(ConflictException exception) {
        return Status.FAILED_PRECONDITION.withDescription(exception.getMessage()).asRuntimeException();
    }

    @GrpcExceptionHandler
    public StatusRuntimeException invalidArgument(IllegalArgumentException exception) {
        return Status.INVALID_ARGUMENT.withDescription(exception.getMessage()).asRuntimeException();
    }
}
