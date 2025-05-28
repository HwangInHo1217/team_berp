package com.team.berp.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "workplace")
@Getter
@Setter
public class Workplace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="work_id")
    private Long workId;

    @Column(name = "work_name", nullable = false)
    private String workName;

    @Column(name = "work_ceonm", nullable = false)
    private String workCeonm;

    @Column(name = "work_no", nullable = false, unique = true)
    private String workNo;

    @Column(name = "work_cond", nullable = false)
    private String workCond;

    @Column(name = "work_item", nullable = false)
    private String workItem;

    @Column(name = "work_tel", nullable = false)
    private String workTel;

    @Column(name = "work_fax", nullable = false)
    private String workFax;

    @Column(name = "work_addr", nullable = false)
    private String workAddr;

    @Column(name = "work_mainadd", nullable = false)
    private String workMainadd;

    @Column(name = "work_detailadd", nullable = false)
    private String workDetailadd;

    @Column(name = "work_manname", nullable = false)
    private String workManname;

    @Column(name = "work_manemail", nullable = false)
    private String workManemail;

    @Column(name = "work_mantel", nullable = false)
    private String workMantel;
}

