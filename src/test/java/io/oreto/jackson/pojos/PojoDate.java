package io.oreto.jackson.pojos;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

public class PojoDate {
    private Date date;
    private java.sql.Date sqlDate;
    private LocalDate localDate;
    private LocalDateTime localDateTime;
    private java.sql.Time time;

    public Date getDate() {
        return date;
    }
    public java.sql.Date getSqlDate() {
        return sqlDate;
    }
    public LocalDate getLocalDate() {
        return localDate;
    }
    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }
    public java.sql.Time getTime() { return time; }

    public void setDate(Date date) {
        this.date = date;
    }
    public void setSqlDate(java.sql.Date sqlDate) {
        this.sqlDate = sqlDate;
    }
    public void setLocalDate(LocalDate localDate) {
        this.localDate = localDate;
    }
    public void setLocalDateTime(LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }
    public void setTime(java.sql.Time time) {
        this.time = time;
    }
}
