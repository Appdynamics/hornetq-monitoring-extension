/*
 * Copyright 2015. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */
package com.appdynamics.extensions.hornetq.config;

/**
 * Created by balakrishnav on 2/4/15.
 */
public enum HornetQMBeanKeyPropertyEnum {
    TYPE("type"),
    ADDRESS("address"),
    MODULE("module"),
    NAME("name");

    private String name;

    private HornetQMBeanKeyPropertyEnum(String name) {
        this.name = name;
    }

    public String toString(){
        return name;
    }
}
