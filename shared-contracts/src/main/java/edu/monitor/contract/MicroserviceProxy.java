package edu.monitor.contract;

public interface MicroserviceProxy<T> {
  T execute(String operation, Object... params);
}
