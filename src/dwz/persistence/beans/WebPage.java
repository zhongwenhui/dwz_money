﻿package dwz.persistence.beans;

import java.util.Date;

/**
 * WebPage generated by MyEclipse Persistence Tools
 */

public class WebPage implements java.io.Serializable {

	// Fields

	private Integer	id;
	private String	name;
	private String	title;
	private String	metaKeywords;
	private String	metaDescription;
	private String	content;
	private String	target;
	private Date	insertDate;
	private Date	updateDate;
	private Integer sequence;

	// Constructors

	public Integer getSequence() {
		return sequence;
	}

	public void setSequence(Integer sequence) {
		this.sequence = sequence;
	}

	/** default constructor */
	public WebPage() {
	}

	/** minimal constructor */
	public WebPage(String name, Date insertDate) {
		this.name = name;
		this.insertDate = insertDate;
	}

	/** full constructor */
	public WebPage(String name, String title, String metaKeywords,
			String metaDescription, String content, String target, Date insertDate,
			Date updateDate, Integer sequence) {
		this.name = name;
		this.title = title;
		this.metaKeywords = metaKeywords;
		this.metaDescription = metaDescription;
		this.content = content;
		this.target = target;
		this.insertDate = insertDate;
		this.updateDate = updateDate;
		this.sequence = sequence;
	}

	// Property accessors

	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getTitle() {
		return this.title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getMetaKeywords() {
		return this.metaKeywords;
	}

	public void setMetaKeywords(String metaKeywords) {
		this.metaKeywords = metaKeywords;
	}

	public String getMetaDescription() {
		return this.metaDescription;
	}

	public void setMetaDescription(String metaDescription) {
		this.metaDescription = metaDescription;
	}

	public String getContent() {
		return this.content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Date getInsertDate() {
		return this.insertDate;
	}

	public void setInsertDate(Date insertDate) {
		this.insertDate = insertDate;
	}

	public Date getUpdateDate() {
		return this.updateDate;
	}

	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

}