package com.retain2;

public interface DeleteHandlerInterface
{
	public static interface OnDeleteItemListener
	{
		public abstract void onDeleteItem(long rowId);
	}
}
