CMAKE_MINIMUM_REQUIRED(VERSION 3.18.1)
PROJECT(gemicom)

SET(PARSER_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../../parser)

ADD_LIBRARY(
    gemicom
    SHARED
    ${PARSER_DIR}/gemicom.cpp
    ${PARSER_DIR}/gemtext.tab.c
    ${PARSER_DIR}/lex.yy.c
)

FIND_LIBRARY(log-lib log)
TARGET_LINK_LIBRARIES(gemicom ${log-lib})
